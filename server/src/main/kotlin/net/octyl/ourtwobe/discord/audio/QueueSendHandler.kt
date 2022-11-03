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

package net.octyl.ourtwobe.discord.audio

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.transform
import mu.KotlinLogging
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.octyl.ourtwobe.datapipe.DataPipeEvent
import net.octyl.ourtwobe.datapipe.PlayableItem
import net.octyl.ourtwobe.discord.PlayerCommand
import net.octyl.ourtwobe.youtube.audio.YouTubeOpusAudioBufferSource
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class QueueSendHandler(
    cookiesPath: Path?,
    guildId: String,
) : AudioSendHandler {
    private val logger = KotlinLogging.logger("${javaClass.name}.$guildId")

    private val audioBufferSource = YouTubeOpusAudioBufferSource(cookiesPath)

    // small hand-off queue that keeps progress close to what it actually is,
    // but allows for some lee-way between the two
    private val audioQueue = Channel<ByteBuffer>(capacity = (TimeUnit.MILLISECONDS.toMillis(40L) / 20L).toInt())

    @OptIn(FlowPreview::class)
    fun play(
        playableItems: Flow<PlayableItem>,
        skipFlow: Flow<PlayerCommand.Skip>,
        volumeStateFlow: StateFlow<Double>
    ): Flow<DataPipeEvent.ProgressItem> {
        return flow {
            coroutineScope {
                // Prepare hot flows so the audio is ready ASAP
                playableItems.collect {
                    emit(
                        it to audioBufferSource.provideAudio(it.youtubeId, volumeStateFlow)
                            .produceIn(this).consumeAsFlow()
                    )
                }
            }
        }
            // only keep 1 song ready
            .buffer(Channel.RENDEZVOUS)
            .transform { (playableItem, audioFlow) ->
                logger.info("Playing '${playableItem.title}' (${playableItem.youtubeId})")
                var base = DataPipeEvent.ProgressItem(playableItem, 0.0)
                try {
                    // number of milliseconds needed for an 0.01% increase
                    val totalMillis = playableItem.duration.toMillis()
                    var millis = 0L
                    var lastPercent = 0.0
                    coroutineScope {
                        val skipChannel = skipFlow.produceIn(this)
                        audioFlow.collect {
                            val result = skipChannel.tryReceive()
                            when {
                                result.isSuccess -> {
                                    // the song is over, kill this coroutine
                                    throw PurposefulCancellationException()
                                }

                                result.isClosed -> error("canceledChannel should never close")
                                result.isFailure -> {
                                    // Fall-through
                                }
                            }
                            millis += 20
                            val percent = (100 * millis.toDouble()) / totalMillis
                            if (percent - lastPercent > 0.1) {
                                lastPercent = percent
                                if (percent >= 100) {
                                    // don't report the end yet
                                    skipChannel.cancel()
                                    return@collect
                                }
                                base = base.copy(progress = percent)
                            }
                            emit(base to it)
                        }
                        skipChannel.cancel()
                    }
                } catch (e: PurposefulCancellationException) {
                    // no big deal
                    logger.info("Skipped '${playableItem.title}' (${playableItem.youtubeId})")
                } finally {
                    emit(base.copy(progress = 100.0) to null)
                    logger.info("Finished '${playableItem.title}' (${playableItem.youtubeId})")
                }
            }
            // store 0.5s worth of audio in queue
            .buffer((TimeUnit.MILLISECONDS.toMillis(500L) / 20L).toInt())
            .map { (update, audio) ->
                audio?.let {
                    audioQueue.send(it)
                }
                update
            }
            .distinctUntilChanged()
    }

    override fun canProvide() = true

    override fun provide20MsAudio(): ByteBuffer? = audioQueue.tryReceive()
        .onClosed { if (it != null) throw it }
        .getOrNull()

    override fun isOpus() = true

}

private class PurposefulCancellationException : CancellationException()
