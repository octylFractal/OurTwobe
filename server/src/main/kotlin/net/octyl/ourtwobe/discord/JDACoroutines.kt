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

package net.octyl.ourtwobe.discord

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import net.dv8tion.jda.api.utils.concurrent.Task
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("DeferredIsResult")
fun <T> Task<T>.asDeferred(parent: Job? = null): Deferred<T?> {
    val canceled = AtomicBoolean()
    val deferred = CompletableDeferred<T>(parent)
    deferred.invokeOnCompletion {
        if (it is CancellationException && canceled.compareAndSet(false, true)) {
            // Cancel the task if the deferred is canceled
            cancel()
        }
    }
    onSuccess {
        deferred.complete(it)
    }
    onError {
        if (it is CancellationException && canceled.compareAndSet(false, true)) {
            // Task.cancel was called, forward to deferred
            deferred.cancel()
        } else {
            deferred.completeExceptionally(it)
        }
    }
    return deferred
}
