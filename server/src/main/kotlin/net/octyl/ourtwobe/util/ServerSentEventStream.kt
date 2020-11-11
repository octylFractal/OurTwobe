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
