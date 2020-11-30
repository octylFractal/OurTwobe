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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import net.octyl.ourtwobe.util.Event
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Each instance is serving one SSE connection.
 */
class DataPipe : AutoCloseable {
    private val messageChannel = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    private val closed = AtomicBoolean()
    private var invokeOnClose = CopyOnWriteArrayList<() -> Unit>()

    /**
     * One-time flow that will produce all messages.
     */
    fun consumeMessages(): Flow<Event> = messageChannel

    suspend fun sendData(data: DataPipeEvent) {
        messageChannel.emit(Event.Data(
            eventType = data.eventType,
            data = data,
        ))
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runBlocking {
                messageChannel.emit(Event.Close)
            }
            invokeOnClose.forEach {
                it()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun invokeOnClose(block: () -> Unit) {
        invokeOnClose.add(block)
    }

}
