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

import org.bytedeco.ffmpeg.avformat.Read_packet_Pointer_BytePointer_int
import org.bytedeco.ffmpeg.avformat.Seek_Pointer_long_int
import org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int
import org.bytedeco.ffmpeg.global.avformat.AVSEEK_SIZE
import org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF
import org.bytedeco.ffmpeg.global.avutil.AVERROR_UNKNOWN
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import org.slf4j.LoggerFactory
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SeekableByteChannel
import java.nio.channels.WritableByteChannel

internal object ByteChannelAvioCallbacks {
    private val LOGGER = LoggerFactory.getLogger(ByteChannelAvioCallbacks::class.java)

    internal class ReadCallback(private val channel: ReadableByteChannel) : Read_packet_Pointer_BytePointer_int() {
        override fun call(opaque: Pointer?, buf: BytePointer, buf_size: Int): Int {
            return try {
                val read = channel.read(buf.limit(buf_size.toLong()).asBuffer())
                if (read == -1) {
                    AVERROR_EOF
                } else read
            } catch (t: Throwable) {
                LOGGER.warn("Error reading from channel", t)
                AVERROR_UNKNOWN
            }
        }
    }

    internal class WriteCallback(private val channel: WritableByteChannel) : Write_packet_Pointer_BytePointer_int() {
        override fun call(opaque: Pointer?, buf: BytePointer, buf_size: Int): Int {
            return try {
                channel.write(buf.limit(buf_size.toLong()).asBuffer().asReadOnlyBuffer())
            } catch (t: Throwable) {
                LOGGER.warn("Error writing to channel", t)
                AVERROR_UNKNOWN
            }
        }
    }

    internal class SeekCallback(private val channel: SeekableByteChannel) : Seek_Pointer_long_int() {
        companion object {
            private const val SEEK_SET = 0
            private const val SEEK_CUR = 1
            private const val SEEK_END = 2
        }

        override fun call(opaque: Pointer?, offset: Long, whence: Int): Long {
            return try {
                val base = when (whence) {
                    SEEK_SET -> 0
                    SEEK_CUR -> channel.position()
                    SEEK_END -> channel.size()
                    AVSEEK_SIZE -> return channel.size()
                    else -> return AVERROR_UNKNOWN.toLong()
                }
                channel.position(base + offset)
                0
            } catch (t: Throwable) {
                LOGGER.warn("Error seeking in channel", t)
                AVERROR_UNKNOWN.toLong()
            }
        }
    }
}
