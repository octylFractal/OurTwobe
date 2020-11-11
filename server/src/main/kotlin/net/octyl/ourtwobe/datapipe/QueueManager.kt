/*
 * Copyright (c) Octavia Togami <https://octyl.net>
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.octyl.ourtwobe.datapipe

import com.google.common.collect.Multimaps
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.selects.select
import net.octyl.ourtwobe.util.RWLock
import net.octyl.ourtwobe.util.read
import net.octyl.ourtwobe.util.write
import java.util.TreeSet

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

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun remove(currentOwners: suspend ProducerScope<Set<String>>.() -> Unit): PlayableItem {
        return coroutineScope {
            val ownerChannel = produce(block = currentOwners)
            var owners = ownerChannel.receive()
            var value: PlayableItem? = null
            while (true) {
                val deferred: CompletableDeferred<Unit> = rwLock.write {
                    removeAvailableItem(owners)?.let {
                        ownerChannel.cancel()
                        value = it
                        return@write null
                    }
                    // put a pending marker into the list
                    CompletableDeferred<Unit>(coroutineContext.job).also {
                        awaitingNewItem.add(it)
                    }
                } ?: break
                // wait to be signalled
                select<Unit> {
                    ownerChannel.onReceive {
                        deferred.cancel()
                        owners = it
                    }
                    deferred.onAwait { Unit }
                }
                // then loop!
            }
            value ?: error("Somehow, no value was set.")
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
