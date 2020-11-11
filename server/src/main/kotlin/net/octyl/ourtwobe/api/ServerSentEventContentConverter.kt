package net.octyl.ourtwobe.api

import io.ktor.application.ApplicationCall
import io.ktor.features.ContentConverter
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.content.WriterContent
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.octyl.ourtwobe.util.Event
import net.octyl.ourtwobe.util.ServerSentEventStream
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class ServerSentEventContentConverter(
    private val config: Configuration
) : ContentConverter {
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
                coroutineScope {
                    launch { stream.sendKeepAliveForever(config.keepAliveInterval) }
                    value.flow.cancellable().collect(stream::sendEvent)
                }
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

class Configuration {
    var keepAliveInterval: Duration = Duration.ofSeconds(30L)
}

fun ContentNegotiation.Configuration.serverSentEvents(
    contentType: ContentType = ContentType.Text.EventStream,
    block: Configuration.() -> Unit = {}) {
    val config = Configuration().also(block)
    register(contentType, ServerSentEventContentConverter(config))
}
