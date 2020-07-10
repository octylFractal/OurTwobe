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

package net.octyl.ourtwobe

import com.google.api.core.ApiFuture
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.google.cloud.firestore.WriteBatch
import com.google.cloud.firestore.WriteResult
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun initFirebase(): FirebaseApp {
    val credentials = Files.newInputStream(Path.of("./secrets/firebase-adminsdk.json"))
        .use(GoogleCredentials::fromStream)

    val options = FirebaseOptions.builder()
        .setCredentials(credentials)
        .build()

    return FirebaseApp.initializeApp(options)
}

suspend fun <R> ApiFuture<R>.await(): R {
    return suspendCancellableCoroutine { cont ->
        addListener({
            try {
                cont.resume(get())
            } catch (e: Throwable) {
                val original = (e as? ExecutionException)?.cause ?: e
                cont.resumeWithException(original)
            }
        }, MoreExecutors.directExecutor())
    }
}

inline fun Firestore.batch(block: BatchScope.() -> Unit): ApiFuture<List<WriteResult>> {
    return batch()
        .also {
            BatchScope(it).block()
        }
        .commit()
}

class BatchScope(private val batch: WriteBatch) {
    fun DocumentReference.deleteInBatch() = batch.delete(this)

    fun DocumentReference.setInBatch(map: Map<String, Any>, options: SetOptions? = null) =
        when (options) {
            null -> batch.set(this, map)
            else -> batch.set(this, map, options)
        }
}
