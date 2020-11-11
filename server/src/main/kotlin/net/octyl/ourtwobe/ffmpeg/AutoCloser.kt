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
