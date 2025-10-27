package net.octyl.ourtwobe.files

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import net.octyl.ourtwobe.datapipe.FileContentKey
import net.octyl.ourtwobe.ffmpeg.AvioCallbacks
import net.octyl.ourtwobe.ffmpeg.FFmpegOpusReencoder
import java.nio.ByteBuffer

class FileOpusAudioBufferSource {
    fun provideAudio(contentKey: FileContentKey, volumeStateFlow: StateFlow<Double>): Flow<ByteBuffer> {
        return flow {
            coroutineScope {
                FFmpegOpusReencoder(
                    contentKey.filename,
                    AvioCallbacks.forReadingByteBuffer(contentKey.fileContent),
                ).use {
                    emitAll(it.recode(volumeStateFlow))
                }
            }
        }
    }
}
