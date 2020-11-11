package net.octyl.ourtwobe.discord.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.octyl.ourtwobe.datapipe.DataPipeEvent
import net.octyl.ourtwobe.datapipe.PlayableItem
import net.octyl.ourtwobe.youtube.audio.YouTubeOpusAudioBufferSource
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class QueueSendHandler : AudioSendHandler {
    // store 20s worth of audio in queue
    private val audioQueue = Channel<ByteBuffer>((TimeUnit.SECONDS.toMillis(20L) / 20L).toInt())

    fun play(playableItem: PlayableItem): Flow<DataPipeEvent.ProgressItem> {
        return flow {
            val base = DataPipeEvent.ProgressItem(playableItem, 0.0)
            emit(base)
            try {
                // number of milliseconds needed for an 0.01% increase
                val millisNeededForIncrement = (
                    playableItem.duration.toMillis().toBigDecimal() * BigDecimal("0.0001")
                    ).toInt()
                    .coerceAtLeast(1)
                var millis = 0L
                var lastMillis = 0L
                var percentageCount = 0
                YouTubeOpusAudioBufferSource.provideAudio(playableItem.youtubeId).collect {
                    audioQueue.send(it)
                    millis += 20
                    if (millis - lastMillis > millisNeededForIncrement) {
                        lastMillis = millis
                        percentageCount++
                        val progress = 0.0001 * percentageCount
                        if (progress >= 100) {
                            // don't report the end yet
                            return@collect
                        }
                        emit(base.copy(progress = progress))
                    }
                }
            } finally {
                emit(base.copy(progress = 100.0))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun canProvide() = !audioQueue.isEmpty

    override fun provide20MsAudio(): ByteBuffer? = audioQueue.poll()

    override fun isOpus() = true

}