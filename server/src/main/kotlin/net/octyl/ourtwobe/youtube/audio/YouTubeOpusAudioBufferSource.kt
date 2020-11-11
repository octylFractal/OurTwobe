package net.octyl.ourtwobe.youtube.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import net.octyl.ourtwobe.ffmpeg.AvioCallbacks
import net.octyl.ourtwobe.ffmpeg.FFmpegOpusReencoder
import java.nio.ByteBuffer
import java.nio.channels.Channels

object YouTubeOpusAudioBufferSource {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun provideAudio(id: String): Flow<ByteBuffer> {
        return flow {
            YouTubeDlProcessBinding(id).use { ytdl ->
                FFmpegOpusReencoder(
                    id,
                    AvioCallbacks.forChannel(Channels.newChannel(ytdl.process.inputStream))
                ).use {
                    emitAll(it.recode())
                }
            }
        }
    }

}
