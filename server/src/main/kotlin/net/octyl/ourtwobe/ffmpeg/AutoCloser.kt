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

import java.util.Deque
import java.util.LinkedList

/**
 * [AutoCloseable]-compatible closer.
 */
class AutoCloser : AutoCloseable {
    // LIFO queue, the last thing registered is the first thing closed
    private val closeables: Deque<AutoCloseable> = LinkedList()
    fun <C : AutoCloseable?> register(autoCloseable: C): C {
        if (autoCloseable != null) {
            closeables.addFirst(autoCloseable)
        }
        return autoCloseable
    }

    inline fun <C, D : C?> register(reference: D, crossinline closer: (C) -> Unit): D {
        if (reference != null) {
            register(AutoCloseable { closer(reference) })
        }
        return reference
    }

    @Throws(Exception::class)
    override fun close() {
        var rethrow: Exception? = null
        for (closeable in closeables) {
            try {
                closeable.close()
            } catch (t: Exception) {
                if (rethrow == null) {
                    rethrow = t
                } else {
                    rethrow.addSuppressed(t)
                }
            }
        }
        closeables.clear()
        if (rethrow != null) {
            throw rethrow
        }
    }
}
