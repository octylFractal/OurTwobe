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

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import net.octyl.ourtwobe.util.Event
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

private val KEEP_ALIVE_DURATION = Duration.ofSeconds(5L)

/**
 * Each instance is serving one SSE connection.
 */
class DataPipe : AutoCloseable {
    private val messageChannel = Channel<Event>(capacity = 64)
    private val closed = AtomicBoolean()

    /**
     * One-time flow that will produce all messages.
     */
    @OptIn(FlowPreview::class)
    fun consumeMessages(): Flow<Event> = flowOf(
        keepAliveFlow(),
        messageChannel.consumeAsFlow()
    )
        .flattenMerge(concurrency = 2)

    private fun keepAliveFlow(): Flow<Event> {
        return flow {
            while (currentCoroutineContext().isActive) {
                // Send KA event with 2x duration, so we have a decent buffer
                emit(serializeEvent(DataPipeEvent.KeepAlive(
                    Instant.now().plus(KEEP_ALIVE_DURATION.multipliedBy(2))
                )))
                delay(KEEP_ALIVE_DURATION)
            }
        }
    }

    suspend fun sendData(data: DataPipeEvent) {
        messageChannel.send(serializeEvent(data))
    }

    private fun serializeEvent(data: DataPipeEvent) = Event.Data(
        id = UUID.randomUUID().toString(),
        eventType = data.eventType,
        data = data,
    )

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runBlocking {
                messageChannel.send(Event.Close)
            }
        }
    }

}
