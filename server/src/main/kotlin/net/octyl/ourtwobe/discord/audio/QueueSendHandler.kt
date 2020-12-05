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

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.octyl.ourtwobe.datapipe.DataPipeEvent
import net.octyl.ourtwobe.datapipe.PlayableItem
import net.octyl.ourtwobe.youtube.audio.YouTubeOpusAudioBufferSource
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class QueueSendHandler : AudioSendHandler {
    // small hand-off queue that keeps progress close to what it actually is,
    // but allows for some lee-way between the two
    private val audioQueue = Channel<ByteBuffer>(capacity = (TimeUnit.MILLISECONDS.toMillis(40L) / 20L).toInt())

    fun play(playableItems: Flow<PlayableItem>, volumeStateFlow: StateFlow<Double>): Flow<DataPipeEvent.ProgressItem> {
        return playableItems
            .transform { playableItem ->
                var base = DataPipeEvent.ProgressItem(playableItem, 0.0)
                try {
                    // number of milliseconds needed for an 0.01% increase
                    val totalMillis = playableItem.duration.toMillis()
                    var millis = 0L
                    var lastPercent = 0.0
                    YouTubeOpusAudioBufferSource.provideAudio(playableItem.youtubeId, volumeStateFlow)
                        .collect {
                            millis += 20
                            val percent = (100 * millis.toDouble()) / totalMillis
                            if (percent - lastPercent > 0.01) {
                                lastPercent = percent
                                if (percent >= 100) {
                                    // don't report the end yet
                                    return@collect
                                }
                                base = base.copy(progress = percent)
                            }
                            emit(base to it)
                        }
                } finally {
                    emit(base.copy(progress = 100.0) to null)
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
    }

    override fun canProvide() = true

    override fun provide20MsAudio(): ByteBuffer? = audioQueue.poll()

    override fun isOpus() = true

}
