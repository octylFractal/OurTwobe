/*
 * This file is part of beat-dropper, licensed under the MIT License (MIT).
 *
 * Copyright (c) Octavia Togami <https://octyl.net>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.octyl.ourtwobe.ffmpeg

import com.google.common.base.Throwables
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import net.dv8tion.jda.api.audio.OpusPacket
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.avcodec
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
import org.bytedeco.ffmpeg.global.avutil.*
import org.bytedeco.ffmpeg.presets.avutil
import org.lwjgl.system.MemoryUtil
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.UnsupportedAudioFileException

/**
 * FFmpeg based re-encoder that produces Discord-compatible Opus packets.
 */
class FFmpegOpusReencoder(
    name: String,
    ioCallbacks: AvioCallbacks
) : AutoCloseable {

    private val closer = AutoCloser()
    private val ctx = closer.register(avformat_alloc_context(), { s -> avformat_free_context(s) })
        ?: error("Unable to allocate context")
    private val decoderCtx: AVCodecContext
    private val encoderCtx: AVCodecContext
    private val frame: AVFrame
    private val packet: AVPacket
    private val audioStreamIndex: Int
    private val resampler: Resampler
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

            val codec = avcodec.avcodec_find_encoder(avcodec.AV_CODEC_ID_OPUS)
                ?: error("Could not find encoder for " + avcodec.avcodec_get_name(avcodec.AV_CODEC_ID_OPUS))

            val supportedFmts = codec.sample_fmts()
            val desiredFormat = generateSequence(0L) { it + 1 }
                .map { supportedFmts[it] }
                .takeWhile { fmt -> fmt != -1 }
                .maxByOrNull { fmt ->
                    val bytes = av_get_bytes_per_sample(fmt)
                    val planar = when (av_get_planar_sample_fmt(fmt)) {
                        0 -> 0
                        else -> 1
                    }
                    val baseScore = when (bytes) {
                        // prefer 2 bytes / 16 bits (our current format)
                        2 -> Int.MAX_VALUE
                        else -> bytes * 2
                    }

                    // prefer non-planar slightly
                    baseScore - planar
                } ?: error("No formats available")

            encoderCtx = closer.register(
                avcodec_alloc_context3(codec),
                { avctx -> avcodec_free_context(avctx) }
            ) ?: error("Unable to allocate codec context")
            val sampleRate = OpusPacket.OPUS_SAMPLE_RATE
            encoderCtx
                .sample_fmt(desiredFormat)
                .sample_rate(sampleRate)
                .channels(2)
                .channel_layout(AV_CH_LAYOUT_STEREO)
                .frame_size(OpusPacket.OPUS_FRAME_SIZE)
                .time_base(av_make_q(1, sampleRate))

            error = avcodec_open2(encoderCtx, codec, null as AVDictionary?)
            check(error == 0) { "Unable to open encoder: " + avErr2Str(error) }

            resampler = closer.register(Resampler(
                Format(
                    channelLayout = audioStream.codecpar().channel_layout(),
                    sampleFormat = audioStream.codecpar().format(),
                    timeBase = audioStream.time_base(),
                    sampleRate = audioStream.codecpar().sample_rate()
                ),
                Format(
                    channelLayout = encoderCtx.channel_layout(),
                    sampleFormat = encoderCtx.sample_fmt(),
                    timeBase = encoderCtx.time_base(),
                    sampleRate = sampleRate
                )
            ))

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
                { pkt-> av_packet_free(pkt) }
            ) ?: error("Unable to allocate packet")

            decoderCtx.pkt_timebase(audioStream.time_base())
        } catch (t: Throwable) {
            closeSilently(t)
            throw t
        }
    }

    fun recode(): Flow<ByteBuffer> {
        return readPackets()
                .map {
                    // we must copy the frame before buffering to avoid use-after-free
                    val frame = av_frame_alloc()
                    av_frame_ref(frame, it)
                    frame
                }
                .buffer()
                .transform {
                    try {
                        emitAll(writePacket(it))
                    } finally {
                        av_frame_free(it)
                    }
                }
    }

    private fun readPackets(): Flow<AVFrame> {
        return flow {
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
                        emitAll(resampler.pushFrame(frame))
                    }
                } finally {
                    av_packet_unref(packet)
                }
            }
            emitAll(resampler.pushFinalFrame(frame.pts()))
        }
    }

    private fun writePacket(frame: AVFrame): Flow<ByteBuffer> {
        return flow {
            var error = avcodec.avcodec_send_frame(encoderCtx, frame)
            if (error != 0) {
                throw IOException("Unable to encode frame: " + avErr2Str(error))
            }
            val outputPacket = av_packet_alloc()
            while (true) {
                error = avcodec.avcodec_receive_packet(encoderCtx, outputPacket)
                if (error == avutil.AVERROR_EAGAIN() || error == AVERROR_EOF) {
                    // need more input!
                    break
                }
                if (error < 0) {
                    throw IOException("Error encoding audio frame: " + avErr2Str(error))
                }
                val buffer = MemoryUtil.memByteBuffer(outputPacket.buf().data().address(), outputPacket.buf().size())
                val dataCopy = ByteBuffer.allocate(buffer.remaining())
                dataCopy.put(buffer)
                dataCopy.flip()
                emit(dataCopy)
                av_packet_unref(outputPacket)
            }
            av_packet_free(outputPacket)
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
