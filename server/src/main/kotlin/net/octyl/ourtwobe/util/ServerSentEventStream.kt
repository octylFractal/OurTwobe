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

import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.octyl.ourtwobe.JACKSON

/**
 * An abstraction over SSE. Also handles keep-alives.
 */
class ServerSentEventStream(
    private val response: ByteWriteChannel,
) {

    private suspend fun writeField(name: String, value: String) {
        for (line in value.lineSequence()) {
            response.writeStringUtf8(name)
            response.writeStringUtf8(": ")
            response.writeStringUtf8(line)
            response.writeStringUtf8("\n")
        }
    }

    suspend fun sendEvent(event: Event) {
        withContext(Dispatchers.IO) {
            exhaustive(when (event) {
                is Event.Close -> {
                    response.flushAndClose()
                    return@withContext
                }
                is Event.Comment -> {
                    writeField("", event.commentText)
                }
                is Event.Data -> {
                    writeField("event", event.eventType)
                    writeField("data", JACKSON.writeValueAsString(event.data))
                    event.id?.let {
                        writeField("id", it)
                    }
                }
            })

            // finish out the event!
            response.writeStringUtf8("\n")
            response.flush()
        }
    }
}

sealed class Event {
    data object Close : Event()

    data class Comment(
        val commentText: String
    ) : Event()

    data class Data(
        val eventType: String,
        val data: Any,
        val id: String? = null,
    ) : Event()
}
