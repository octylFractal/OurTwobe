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

import org.bytedeco.ffmpeg.avformat.AVIOContext
import org.bytedeco.ffmpeg.avformat.Read_packet_Pointer_BytePointer_int
import org.bytedeco.ffmpeg.avformat.Seek_Pointer_long_int
import org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int
import org.bytedeco.ffmpeg.global.avformat
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import java.nio.channels.Channel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SeekableByteChannel
import java.nio.channels.WritableByteChannel

/**
 * Stores a set of AVIO callbacks. This class will [retain them][Pointer.retainReference]
 * until closed.
 *
 * @param resource the resource that is associated with the callbacks, and should be closed after them
 */
class AvioCallbacks(
    private val readCallback: Read_packet_Pointer_BytePointer_int?,
    private val writeCallback: Write_packet_Pointer_BytePointer_int?,
    private val seekCallback: Seek_Pointer_long_int?,
    resource: AutoCloseable?
) : AutoCloseable {
    private val closer = AutoCloser()

    init {
        readCallback?.let { closer.register(it).retainReference<Pointer>() }
        writeCallback?.let { closer.register(it).retainReference<Pointer>() }
        seekCallback?.let { closer.register(it).retainReference<Pointer>() }
        closer.register(resource)
    }

    /**
     * Allocate a context with the callbacks in this object.
     *
     *
     *
     * Will *not* free the context automatically, that is up to you.
     *
     *
     * @return AVIO context if allocated, `null` if failed
     */
    fun allocateContext(bufferSize: Int, writable: Boolean): AVIOContext? {
        return avformat.avio_alloc_context(
                BytePointer(avutil.av_malloc(bufferSize.toLong())), bufferSize, if (writable) 1 else 0, null,
                readCallback, writeCallback, seekCallback
        )
    }

    @Throws(Exception::class)
    override fun close() {
        closer.close()
    }

    companion object {
        fun forChannel(channel: Channel?): AvioCallbacks {
            return AvioCallbacks(
                    if (channel is ReadableByteChannel) ByteChannelAvioCallbacks.ReadCallback(channel) else null,
                    if (channel is WritableByteChannel) ByteChannelAvioCallbacks.WriteCallback(channel) else null,
                    if (channel is SeekableByteChannel) ByteChannelAvioCallbacks.SeekCallback(channel) else null,
                    channel
            )
        }
    }
}
