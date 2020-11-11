package net.octyl.ourtwobe.datapipe

import com.google.common.collect.Multimaps
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.selects.select
import net.octyl.ourtwobe.util.RWLock
import net.octyl.ourtwobe.util.read
import net.octyl.ourtwobe.util.write
import java.util.TreeSet
import kotlin.coroutines.coroutineContext

class QueueManager {

    private val rwLock = RWLock()
    private val queues = Multimaps.newSortedSetMultimap<String, PlayableItem>(HashMap(), ::TreeSet)
    private val _events = MutableSharedFlow<DataPipeEvent.QueueItem>(extraBufferCapacity = 16)
    val events: SharedFlow<DataPipeEvent.QueueItem> = _events

    private val awaitingNewItem = mutableListOf<CompletableDeferred<Unit>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun insert(owner: String, item: PlayableItem) {
        rwLock.write {
            queues.put(owner, item)
            _events.emit(DataPipeEvent.QueueItem(owner, item))
            awaitingNewItem.forEach {
                it.complete(Unit)
            }
            awaitingNewItem.clear()
        }
    }

    suspend fun getQueues(): Map<String, List<PlayableItem>> {
        rwLock.read {
            return Multimaps.asMap(queues).mapValues {
                it.value.toList()
            }
        }
    }

    suspend fun remove(currentOwners: ReceiveChannel<Set<String>>): PlayableItem {
        var owners = currentOwners.receive()
        while (true) {
            val deferred: CompletableDeferred<Unit> = rwLock.write {
                removeAvailableItem(owners)?.let {
                    currentOwners.cancel()
                    return it
                }
                // put a pending marker into the list
                CompletableDeferred<Unit>(coroutineContext.job).also {
                    awaitingNewItem.add(it)
                }
            }
            // wait to be signalled
            select<Unit> {
                currentOwners.onReceive {
                    deferred.cancel()
                    owners = it
                }
                deferred.onAwait { Unit }
            }
            // then loop!
        }
    }

    private fun removeAvailableItem(activeOwners: Set<String>): PlayableItem? {
        val queue = activeOwners
            .map { queues[it] }
            .filter { it.isNotEmpty() }
            .minByOrNull { it.first() }
            // there were no queues, so no items left
            ?: return null

        // optimized removal (set can already know where the iterator is)
        val iter = queue.iterator()
        val item = iter.next()
        iter.remove()

        return item
    }

}
