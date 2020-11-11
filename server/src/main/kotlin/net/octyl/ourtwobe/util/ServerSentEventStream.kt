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

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.octyl.ourtwobe.JACKSON
import java.io.Writer
import java.time.Duration

/**
 * An abstraction over SSE. Also handles keep-alives.
 */
class ServerSentEventStream(
    private val response: Writer,
) {

    suspend fun sendKeepAliveForever(interval: Duration) {
        withContext(CoroutineName("SSE Keep-Alive")) {
            while (isActive) {
                sendEvent(Event.Comment("keep alive"))
                delay(interval.toMillis())
            }
        }
    }

    private fun writeField(name: String, value: String) {
        for (line in value.lineSequence()) {
            response.write(name)
            response.write(": ")
            response.write(line)
            response.write("\n")
        }
    }

    suspend fun sendEvent(event: Event) {
        withContext(Dispatchers.IO) {
            exhaustive(when (event) {
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
            response.write("\n")
            response.flush()
        }
    }
}

sealed class Event {
    class Comment(
        val commentText: String
    ) : Event()

    class Data(
        val eventType: String,
        val data: Any,
        val id: String? = null,
    ) : Event()
}
