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

package net.octyl.ourtwobe.ffmpeg

import com.google.common.base.Throwables
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.transform
import net.dv8tion.jda.api.audio.OpusPacket
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avutil.AVAudioFifo
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc
import org.bytedeco.ffmpeg.global.avcodec.av_packet_free
import org.bytedeco.ffmpeg.global.avcodec.av_packet_unref
import org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3
import org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder
import org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context
import org.bytedeco.ffmpeg.global.avcodec.avcodec_open2
import org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_to_context
import org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame
import org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet
import org.bytedeco.ffmpeg.global.avformat.av_read_frame
import org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context
import org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info
import org.bytedeco.ffmpeg.global.avformat.avformat_free_context
import org.bytedeco.ffmpeg.global.avformat.avformat_open_input
import org.bytedeco.ffmpeg.global.avutil.AVERROR_EAGAIN
import org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF
import org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO
import org.bytedeco.ffmpeg.global.avutil.AV_CH_LAYOUT_STEREO
import org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S16
import org.bytedeco.ffmpeg.global.avutil.av_audio_fifo_alloc
import org.bytedeco.ffmpeg.global.avutil.av_audio_fifo_free
import org.bytedeco.ffmpeg.global.avutil.av_audio_fifo_read
import org.bytedeco.ffmpeg.global.avutil.av_audio_fifo_size
import org.bytedeco.ffmpeg.global.avutil.av_audio_fifo_write
import org.bytedeco.ffmpeg.global.avutil.av_frame_alloc
import org.bytedeco.ffmpeg.global.avutil.av_frame_free
import org.bytedeco.ffmpeg.global.avutil.av_frame_get_buffer
import org.bytedeco.ffmpeg.global.avutil.av_frame_make_writable
import org.bytedeco.ffmpeg.global.avutil.av_frame_ref
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.opus.Opus.OPUS_APPLICATION_AUDIO
import org.lwjgl.util.opus.Opus.OPUS_OK
import org.lwjgl.util.opus.Opus.opus_encode
import org.lwjgl.util.opus.Opus.opus_encoder_create
import org.lwjgl.util.opus.Opus.opus_encoder_destroy
import org.lwjgl.util.opus.Opus.opus_strerror
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.UnsupportedAudioFileException

/**
 * FFmpeg based re-encoder that produces Discord-compatible Opus packets.
 */
class FFmpegOpusReencoder(
    name: String,
    ioCallbacks: AvioCallbacks,
) : AutoCloseable {

    private val closer = AutoCloser()
    private val ctx = closer.register(avformat_alloc_context(), { s -> avformat_free_context(s) })
        ?: error("Unable to allocate context")
    private val decoderCtx: AVCodecContext
    private val frame: AVFrame
    private val packet: AVPacket
    private val audioStreamIndex: Int
    private val inputFormat: Format
    private val outputFormat: Format
    private val audioFifo: AVAudioFifo
    private val outputFrame: AVFrame
    private val opus: Long
    private val closed = AtomicBoolean()
    private fun closeSilently(cause: Throwable) {
        try {
            close()
        } catch (suppress: Throwable) {
            cause.addSuppressed(suppress)
        }
    }

    init {
        val avioCtx = closer.register(ioCallbacks).allocateContext(4096, false)
            ?: error("Unable to allocate IO context")
        ctx.pb(closer.register(avioCtx))
        var error = avformat_open_input(ctx, name, null, null)
        check(error == 0) { "Error opening input: " + avErr2Str(error) }
        try {
            error = avformat_find_stream_info(ctx, null as AVDictionary?)
            check(error == 0) { "Error finding stream info: " + avErr2Str(error) }

            if (ctx.nb_streams() == 0) {
                throw UnsupportedAudioFileException("No streams detected by FFmpeg")
            }

            audioStreamIndex = (0 until ctx.nb_streams())
                .firstOrNull { streamIdx: Int ->
                    ctx.streams(streamIdx).codecpar().codec_type() == AVMEDIA_TYPE_AUDIO
                }
                ?: throw UnsupportedAudioFileException("No audio stream detected by FFmpeg")

            val audioStream = ctx.streams(audioStreamIndex)

            val desiredFormat = AV_SAMPLE_FMT_S16
            val sampleRate = OpusPacket.OPUS_SAMPLE_RATE

            inputFormat = Format(
                channelLayout = audioStream.codecpar().channel_layout(),
                sampleFormat = audioStream.codecpar().format(),
                timeBase = audioStream.time_base(),
                sampleRate = audioStream.codecpar().sample_rate()
            )
            outputFormat = Format(
                channelLayout = AV_CH_LAYOUT_STEREO,
                sampleFormat = desiredFormat,
                timeBase = audioStream.time_base(),
                sampleRate = sampleRate
            )

            val decoder = avcodec_find_decoder(audioStream.codecpar().codec_id())
                ?: throw UnsupportedAudioFileException("Not decode-able by FFmpeg")

            decoderCtx = closer.register(
                avcodec_alloc_context3(decoder),
                { avctx -> avcodec_free_context(avctx) }
            ) ?: error("Unable to allocate codec context")

            error = avcodec_parameters_to_context(decoderCtx, audioStream.codecpar())
            check(error == 0) { "Error passing parameters: " + avErr2Str(error) }

            error = avcodec_open2(decoderCtx, decoder, null as AVDictionary?)
            check(error == 0) { "Error opening decoder: " + avErr2Str(error) }

            frame = closer.register(
                av_frame_alloc(),
                { frame -> av_frame_free(frame) }
            ) ?: error("Unable to allocate frame")

            packet = closer.register(
                av_packet_alloc(),
                { pkt -> av_packet_free(pkt) }
            ) ?: error("Unable to allocate packet")

            val encoderFrameSize = 960

            audioFifo = closer.register(
                av_audio_fifo_alloc(desiredFormat, 2, encoderFrameSize),
                { fifo -> av_audio_fifo_free(fifo) }
            ) ?: error("Unable to allocate FIFO")

            outputFrame = closer.register(
                av_frame_alloc(),
                { frame -> av_frame_free(frame) }
            ) ?: error("Unable to allocate frame")
            outputFrame.format(desiredFormat)
                .nb_samples(encoderFrameSize)
                .channel_layout(AV_CH_LAYOUT_STEREO)

            error = av_frame_get_buffer(outputFrame, 0)
            if (error != 0) {
                throw IOException("Unable to prepare frame: " + avErr2Str(error))
            }

            decoderCtx.pkt_timebase(audioStream.time_base())

            MemoryStack.stackPush().use { stack ->
                val errors = stack.mallocInt(1)
                val opus = opus_encoder_create(sampleRate, 2, OPUS_APPLICATION_AUDIO, errors)
                if (errors[0] != OPUS_OK) {
                    throw IOException("Unable to create Opus encoder: ${opus_strerror(errors[0])}")
                }
                this.opus = closer.register(
                    opus,
                    { opus_encoder_destroy(it) }
                )
            }
        } catch (t: Throwable) {
            closeSilently(t)
            throw t
        }
    }

    fun recode(volumeStateFlow: StateFlow<Double>): Flow<ByteBuffer> {
        return readPackets(volumeStateFlow)
            .map {
                // we must copy the frame before buffering to avoid use-after-free
                val frame = av_frame_alloc()
                av_frame_ref(frame, it)
                frame
            }
            .buffer(capacity = 4)
            .transform {
                try {
                    emitAll(writePacket(it))
                } finally {
                    av_frame_free(it)
                }
            }
            .onCompletion { cause ->
                // Don't bother writing packets to an already dead stream
                if (cause == null) {
                    emitAll(writePacket(null))
                }
            }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun readPackets(volumeStateFlow: StateFlow<Double>): Flow<AVFrame> {
        return flow {
            coroutineScope {
                val resamplerChannel = volumeStateFlow
                    .map { Resampler(inputFormat, outputFormat, it) }
                    .buffer(Channel.RENDEZVOUS)
                    .produceIn(this)
                var resampler = resamplerChannel.receive()
                try {
                    // packet reading loop
                    readPacket@ while (av_read_frame(ctx, packet) >= 0) {
                        try {
                            if (packet.stream_index() != audioStreamIndex) {
                                continue
                            }
                            var error = avcodec_send_packet(decoderCtx, packet)
                            check(error == 0) { "Error sending packet to decoder: " + avErr2Str(error) }
                            // audio frame reading loop
                            while (true) {
                                error = avcodec_receive_frame(decoderCtx, frame)
                                if (error == AVERROR_EAGAIN() || error == AVERROR_EOF) {
                                    continue@readPacket
                                }
                                check(error == 0) { "Error getting frame from decoder: " + avErr2Str(error) }
                                while (true) {
                                    resamplerChannel.poll()?.let { newResampler ->
                                        // resampler changed since frame push, clear it out
                                        emitAll(resampler.pushFinalFrame(frame.pts()))
                                        resampler.close()
                                        resampler = newResampler
                                    } ?: break
                                }
                                emitAll(resampler.pushFrame(frame))
                            }
                        } finally {
                            av_packet_unref(packet)
                        }
                    }
                    emitAll(resampler.pushFinalFrame(frame.pts()))
                } finally {
                    // Kill channel, we stopped listening
                    resamplerChannel.cancel()
                    resampler.close()
                }
            }
        }
    }

    private fun writePacket(frame: AVFrame?): Flow<ByteBuffer> {
        return flow {
            var error: Int
            if (frame != null) {
                error = av_audio_fifo_write(audioFifo, frame.data(), frame.nb_samples())
                if (error < 0) {
                    throw IOException("Unable to write frame to FIFO: " + avErr2Str(error))
                }
                if (av_audio_fifo_size(audioFifo) < outputFrame.nb_samples()) {
                    // not enough to do encoding yet, ask for more
                    return@flow
                }
            }
            while (true) {
                val size = av_audio_fifo_size(audioFifo)
                if (size < outputFrame.nb_samples()) {
                    break
                }
                error = av_frame_make_writable(outputFrame)
                if (error != 0) {
                    throw IOException("Unable to prepare frame: " + avErr2Str(error))
                }

                error = av_audio_fifo_read(audioFifo, outputFrame.data(), outputFrame.nb_samples())
                if (error < 0) {
                    throw IOException("Unable to read frame: " + avErr2Str(error))
                }

                MemoryStack.stackPush().use { stack ->
                    val data = stack.malloc(4096)
                    val result = opus_encode(
                        opus,
                        MemoryUtil.memShortBuffer(outputFrame.data()[0].address(), outputFrame.linesize(0)),
                        outputFrame.nb_samples(),
                        data
                    )
                    if (result < 0) {
                        throw IOException("Opus encode error: ${opus_strerror(result)}")
                    }
                    data.limit(result)
                    val dataCopy = ByteBuffer.allocate(data.remaining())
                    dataCopy.put(data)
                    dataCopy.flip()
                    emit(dataCopy)
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun close() {
        // check if already closed
        if (closed.get()) {
            return
        }
        // try to acquire exclusive close "lock"
        if (!closed.compareAndSet(false, true)) {
            return
        }
        try {
            closer.close()
        } catch (e: Exception) {
            Throwables.propagateIfPossible(e, IOException::class.java)
            Throwables.throwIfUnchecked(e)
            throw RuntimeException(e)
        }
    }
}
