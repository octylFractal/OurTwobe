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
