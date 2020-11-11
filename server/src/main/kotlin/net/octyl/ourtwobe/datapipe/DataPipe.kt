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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import net.octyl.ourtwobe.discord.DiscordUser
import net.octyl.ourtwobe.util.Event
import net.octyl.ourtwobe.util.ServerSentEventStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Each instance is serving one SSE connection.
 */
class DataPipe(
    val user: DiscordUser,
) : AutoCloseable {
    private val messageChannel = Channel<Event>(Channel.BUFFERED)
    private val closed = AtomicBoolean()

    /**
     * One-time flow that will produce all messages.
     */
    fun consumeMessages(): Flow<Event> = messageChannel.consumeAsFlow()

    suspend fun sendData(data: DataPipeEvent) {
        messageChannel.send(Event.Data(
            eventType = data.eventType,
            data = data.toSerializedForm()
        ))
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            messageChannel.close()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun invokeOnClose(block: () -> Unit) {
        messageChannel.invokeOnClose { block() }
    }

}
