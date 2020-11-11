

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
