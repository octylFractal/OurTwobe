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

package net.octyl.ourtwobe.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A dead-simple RW lock, based on the two-mutex version from Wikipedia.
 */
class RWLock {

    private val readLock = Mutex()
    private val globalLock = Mutex()
    private var readers = 0

    suspend fun lockRead() {
        readLock.withLock {
            readers++
            if (readers == 1) {
                globalLock.lock()
            }
        }
    }

    suspend fun unlockRead() {
        readLock.withLock {
            readers--
            if (readers == 0) {
                globalLock.unlock()
            }
        }
    }

    suspend fun lockWrite() {
        globalLock.lock()
    }

    // we might use suspend in the future, so for API compatibility, we'll mark it suspend
    @Suppress("RedundantSuspendModifier")
    suspend fun unlockWrite() {
        globalLock.unlock()
    }
}

suspend inline fun <R> RWLock.read(block: () -> R): R {
    lockRead()
    try {
        return block()
    } finally {
        unlockRead()
    }
}

suspend inline fun <R> RWLock.write(block: () -> R): R {
    lockWrite()
    try {
        return block()
    } finally {
        unlockWrite()
    }
}
