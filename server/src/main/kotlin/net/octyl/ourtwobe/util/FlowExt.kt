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

package net.octyl.ourtwobe.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
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
            var closed = false
            while (!closed) {
                val chunk = mutableListOf<T>()
                withTimeoutOrNull(maxDuration) {
                    while (true) {
                        val next = channel.receiveCatching()
                        if (next.isClosed) {
                            closed = true
                            break
                        }
                        chunk.add(next.getOrThrow())
                        if (chunk.size >= count) {
                            break
                        }
                    }
                }
                if (chunk.isNotEmpty()) {
                    emit(chunk)
                }
            }
        }
    }
}
