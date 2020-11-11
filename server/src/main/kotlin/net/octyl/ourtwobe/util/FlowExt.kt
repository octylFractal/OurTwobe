package net.octyl.ourtwobe.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.time.withTimeoutOrNull
import java.time.Duration

/**
 * Buffer either up to [count] items or as long as [maxDuration] since the first item,
 * then emit a list containing those items. Never emits an empty list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.nagle(count: Int, maxDuration: Duration): Flow<List<T>> {
    return flow {
        coroutineScope {
            val channel = produce {
                this@nagle.collect {
                    send(it)
                }
            }
            while (!channel.isClosedForReceive) {
                val chunk = mutableListOf<T>()
                withTimeoutOrNull(maxDuration) {
                    for (item in channel) {
                        chunk.add(item)
                        if (chunk.size >= count) break
                    }
                }
                if (chunk.isNotEmpty()) {
                    emit(chunk)
                }
            }
        }
    }
}
