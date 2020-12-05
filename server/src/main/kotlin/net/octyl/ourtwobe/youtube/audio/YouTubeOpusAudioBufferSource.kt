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

package net.octyl.ourtwobe.youtube.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import mu.KotlinLogging
import net.octyl.ourtwobe.ffmpeg.AvioCallbacks
import net.octyl.ourtwobe.ffmpeg.FFmpegOpusReencoder
import java.nio.ByteBuffer
import java.nio.channels.Channels

object YouTubeOpusAudioBufferSource {

    private val logger = KotlinLogging.logger { }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun provideAudio(id: String, volumeStateFlow: StateFlow<Double>): Flow<ByteBuffer> {
        return flow {
            YouTubeDlProcessBinding(id).use { ytdl ->
                val channel = Channels.newChannel(ytdl.process.inputStream)
                FFmpegOpusReencoder(
                    id,
                    AvioCallbacks.forChannel(channel)
                ).use {
                    emitAll(it.recode(volumeStateFlow))
                }
            }
        }
            .retry(5) {
                logger.info(it) { "Failed to play $id, retrying..." }
                delay(1000)
                true
            }
    }

}
