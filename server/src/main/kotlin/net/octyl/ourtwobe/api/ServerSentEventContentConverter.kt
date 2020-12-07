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

package net.octyl.ourtwobe.api

import io.ktor.application.ApplicationCall
import io.ktor.features.ContentConverter
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.content.WriterContent
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import net.octyl.ourtwobe.util.Event
import net.octyl.ourtwobe.util.ServerSentEventStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class ServerSentEventContentConverter : ContentConverter {
    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        // This makes literally zero sense
        return null
    }

    override suspend fun convertForSend(context: PipelineContext<Any, ApplicationCall>, contentType: ContentType, value: Any): Any? {
        if (value !is EventFlow) {
            return null
        }
        return WriterContent({
            value.use {
                val stream = ServerSentEventStream(this)
                value.flow.cancellable().collect(stream::sendEvent)
            }
        }, contentType)
    }
}

class EventFlow(
    val flow: Flow<Event>
) : AutoCloseable {
    private val closed = AtomicBoolean()
    private val closeListeners = CopyOnWriteArrayList<() -> Unit>()

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            for (listener in closeListeners) {
                try {
                    listener()
                } catch (ignored: Throwable) {
                }
            }
            closeListeners.clear()
        }
    }

    fun invokeOnClose(block: () -> Unit) {
        if (!closed.get()) {
            closeListeners.add(block)
            if (closed.get()) {
                // we might have added a listener after the #clear call above...
                closeListeners.remove(block)
            }
        }
    }
}

fun ContentNegotiation.Configuration.serverSentEvents(
    contentType: ContentType = ContentType.Text.EventStream,
) {
    register(contentType, ServerSentEventContentConverter())
}
