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

import io.ktor.http.HttpStatusCode
import mu.KLogger
import mu.KotlinLogging
import net.octyl.ourtwobe.api.ApiError
import net.octyl.ourtwobe.api.ApiErrorException
import net.octyl.ourtwobe.datapipe.MAX_DURATION
import net.octyl.ourtwobe.datapipe.Thumbnail
import org.bytedeco.ffmpeg.avcodec.AVCodec
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avformat.AVStream
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3
import org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder
import org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context
import org.bytedeco.ffmpeg.global.avcodec.avcodec_open2
import org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_to_context
import org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame
import org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet
import org.bytedeco.ffmpeg.global.avformat.AV_DISPOSITION_ATTACHED_PIC
import org.bytedeco.ffmpeg.global.avformat.av_find_best_stream
import org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context
import org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info
import org.bytedeco.ffmpeg.global.avformat.avformat_free_context
import org.bytedeco.ffmpeg.global.avformat.avformat_open_input
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO
import org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_ARGB
import org.bytedeco.ffmpeg.global.avutil.av_dict_get
import org.bytedeco.ffmpeg.global.avutil.av_frame_alloc
import org.bytedeco.ffmpeg.global.avutil.av_frame_free
import org.bytedeco.ffmpeg.global.avutil.av_frame_get_buffer
import org.bytedeco.ffmpeg.global.avutil.av_q2d
import org.bytedeco.ffmpeg.global.swscale
import org.bytedeco.ffmpeg.global.swscale.SWS_BILINEAR
import org.bytedeco.ffmpeg.global.swscale.sws_getContext
import org.bytedeco.javacpp.DoublePointer
import java.awt.image.BufferedImage
import java.nio.ByteOrder
import java.time.Duration

data class FileItemInfo(
    val title: String?,
    val thumbnail: Thumbnail?,
    val duration: Duration,
) {
    companion object {
        fun load(
            name: String,
            ioCallbacks: AvioCallbacks,
        ): FileItemInfo {
            val logger = KotlinLogging.logger("${FileItemInfo::class.java.name}.load(${name})")
            return AutoCloser().use { closer ->
                val ctx = closer.register(avformat_alloc_context(), ::avformat_free_context)
                    ?: error("Unable to allocate context")
                val avioCtx = closer.register(ioCallbacks).allocateContext(4096, false)
                    ?: error("Unable to allocate IO context")
                ctx.pb(closer.register(avioCtx))
                var error = avformat_open_input(ctx, name, null, null)
                check(error == 0) { "Error opening input: " + avErr2Str(error) }
                error = avformat_find_stream_info(ctx, null as AVDictionary?)
                check(error == 0) { "Error finding stream info: " + avErr2Str(error) }
                if (ctx.nb_streams() <= 0) {
                    throw ApiErrorException(
                        ApiError("upload.nostreams", "No streams found in uploaded file"),
                        HttpStatusCode.BadRequest,
                    )
                }
                val audioStream = av_find_best_stream(
                    ctx,
                    AVMEDIA_TYPE_AUDIO,
                    -1,
                    -1,
                    null as AVCodec?,
                    0,
                ).let { idx ->
                    if (idx < 0) {
                        throw ApiErrorException(
                            ApiError("upload.nostreams.audio", "No audio streams found in uploaded file"),
                            HttpStatusCode.BadRequest,
                        )
                    }
                    ctx.streams(idx)
                }
                FileItemInfo(
                    title = findTitle(ctx, audioStream),
                    thumbnail = extractThumbnail(logger, ctx),
                    duration = getDuration(ctx, audioStream),
                )
            }
        }

        fun findTitle(ctx: AVFormatContext, audioStream: AVStream): String? {
            av_dict_get(ctx.metadata(), "title", null, 0)?.value()?.string?.let { containerTitle ->
                return containerTitle
            }
            av_dict_get(audioStream.metadata(), "title", null, 0)?.value()?.string?.let { streamTitle ->
                return streamTitle
            }
            return null
        }

        fun extractThumbnail(logger: KLogger, ctx: AVFormatContext): Thumbnail? {
            return extractThumbnailImage(ctx, logger)?.let { image ->
                Thumbnail.fromBufferedImage(image)
            }
        }

        private fun extractThumbnailImage(ctx: AVFormatContext, logger: KLogger): BufferedImage? {
            // Get "attached pic" stream, which will be the thumbnail if present
            val attachedPicStream = (0..<ctx.nb_streams()).firstNotNullOfOrNull { idx ->
                ctx.streams(idx).takeIf {
                    it.disposition() and AV_DISPOSITION_ATTACHED_PIC != 0
                }
            } ?: return null
            val attachedPicDecoder = avcodec_find_decoder(attachedPicStream.codecpar().codec_id())
            if (attachedPicDecoder == null) {
                logger.warn { "No decoder found for attached pic codec id ${attachedPicStream.codecpar().codec_id()}" }
                return null
            }
            return AutoCloser().use { closer ->
                val codecCtx = closer.register(avcodec_alloc_context3(attachedPicDecoder), ::avcodec_free_context)
                if (codecCtx == null) {
                    logger.warn { "Unable to allocate codec context for attached pic" }
                    return null
                }
                var error = avcodec_parameters_to_context(codecCtx, attachedPicStream.codecpar())
                if (error < 0) {
                    logger.warn {
                        "Unable to copy codec parameters to context for attached pic: ${avErr2Str(error)}"
                    }
                    return null
                }
                error = avcodec_open2(codecCtx, attachedPicDecoder, null as AVDictionary?)
                if (error < 0) {
                    logger.warn { "Unable to open codec for attached pic: ${avErr2Str(error)}" }
                    return null
                }
                error = avcodec_send_packet(codecCtx, attachedPicStream.attached_pic())
                if (error < 0) {
                    logger.warn { "Unable to send packet to decoder for attached pic: ${avErr2Str(error)}" }
                    return null
                }
                val frame = closer.register(av_frame_alloc(), ::av_frame_free)
                if (frame == null) {
                    logger.warn { "Unable to allocate frame for attached pic" }
                    return null
                }
                error = avcodec_receive_frame(codecCtx, frame)
                if (error < 0) {
                    logger.warn { "Unable to receive frame from decoder for attached pic: ${avErr2Str(error)}" }
                    return null
                }
                val targetWidth = 320
                val targetHeight = (frame.height().toDouble() * (targetWidth.toDouble() / frame.width().toDouble())).toInt()
                val swsScaleCtx = sws_getContext(
                    frame.width(),
                    frame.height(),
                    frame.format(),
                    targetWidth,
                    targetHeight,
                    AV_PIX_FMT_ARGB,
                    SWS_BILINEAR,
                    null,
                    null,
                    null as DoublePointer?,
                )
                if (swsScaleCtx == null) {
                    logger.warn { "Unable to create sws scale context for attached pic" }
                    return null
                }
                val resultFrame = closer.register(av_frame_alloc(), ::av_frame_free)
                if (resultFrame == null) {
                    logger.warn { "Unable to allocate result frame for attached pic result" }
                    return null
                }
                resultFrame.format(AV_PIX_FMT_ARGB)
                resultFrame.width(targetWidth)
                resultFrame.height(targetHeight)
                error = av_frame_get_buffer(resultFrame, 0)
                if (error < 0) {
                    logger.warn { "Unable to allocate buffer for attached pic result frame: ${avErr2Str(error)}" }
                    return null
                }
                error = swscale.sws_scale(
                    swsScaleCtx,
                    frame.data(),
                    frame.linesize(),
                    0,
                    frame.height(),
                    resultFrame.data(),
                    resultFrame.linesize(),
                )
                if (error < 0) {
                    logger.warn { "Unable to scale attached pic frame: ${avErr2Str(error)}" }
                    return null
                }
                val image = BufferedImage(
                    resultFrame.width(),
                    resultFrame.height(),
                    BufferedImage.TYPE_INT_ARGB,
                )
                val data = resultFrame.data(0)
                // Get a copy of the data so we have it as an array
                val lineSize = resultFrame.linesize(0)
                val sizeInBytes = lineSize * resultFrame.height()
                val buffer = IntArray((sizeInBytes) / Int.SIZE_BYTES)
                data.limit(sizeInBytes.toLong())
                    .asBuffer()
                    .order(ByteOrder.BIG_ENDIAN)
                    .asIntBuffer()
                    .get(buffer)
                image.setRGB(
                    0, 0,
                    resultFrame.width(), resultFrame.height(),
                    buffer,
                    0,
                    resultFrame.linesize(0) / Int.SIZE_BYTES,
                )
                image
            }
        }

        private val NANOS_IN_SECOND = Duration.ofSeconds(1).toNanos()

        fun getDuration(ctx: AVFormatContext, audioStream: AVStream): Duration {
            val duration = if (audioStream.duration() != avutil.AV_NOPTS_VALUE) {
                audioStream.duration() * av_q2d(audioStream.time_base())
            } else if (ctx.duration() != avutil.AV_NOPTS_VALUE) {
                ctx.duration().toDouble() / avutil.AV_TIME_BASE.toDouble()
            } else {
                return MAX_DURATION
            }
            val durationSeconds = duration.toLong()
            val durationNanos = ((duration - durationSeconds) * NANOS_IN_SECOND).toLong()
            return Duration.ofSeconds(durationSeconds, durationNanos)
        }

    }
}
