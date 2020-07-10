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

package net.octyl.ourtwobe.pipe

import com.google.api.core.ApiFuture
import com.google.cloud.firestore.Firestore
import discord4j.core.DiscordClient
import discord4j.core.event.domain.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import mu.toKLogger
import net.octyl.ourtwobe.await
import net.octyl.ourtwobe.isCancellationCause
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.streams.asSequence

abstract class DiscordEventPipe(
    val firestore: Firestore,
    collectionKey: String
) {
    private val logger = LoggerFactory.getLogger(javaClass).toKLogger()

    protected val collection = firestore.collection(collectionKey)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope, bot: DiscordClient): Job {
        logger.info("Running initial pipe")
        initialPipe(bot)
            .onCompletion { err ->
                if (err == null) {
                    logger.info("Finished piping initial data")
                } else {
                    logger.warn("Failed initial pipe of date, continuing to register event flows", err)
                }
            }
            .launchIn(scope)
        return scope.launch {
            logger.info("Registering event flows")
            EventFlowBuilder(this, bot).registerEventFlows()
        }
    }

    protected abstract fun EventFlowBuilder.registerEventFlows()

    abstract fun initialPipe(bot: DiscordClient): Flow<*>

}

private val STACK_WALKER by lazy(LazyThreadSafetyMode.PUBLICATION) {
    StackWalker.getInstance()
}

private fun callerLogger() = LoggerFactory.getLogger(
    STACK_WALKER.walk { stack ->
        stack.asSequence().elementAt(2)
    }.className
)

/**
 * Await every [ApiFuture] in [this], and return the result, regardless of failure or success.
 */
fun <T> Flow<ApiFuture<T>>.awaitAllSafe(): Flow<Result<T>> {
    val logger = callerLogger()
    return map {
        try {
            Result.success(it.await())
        } catch (err: Throwable) {
            if (err.isCancellationCause()) {
                throw err
            }
            logger.warn("Error awaiting Firestore future", err)
            Result.failure(err)
        }
    }
}

class EventFlowBuilder(
    private val scope: CoroutineScope,
    private val bot: DiscordClient
) {
    fun <E : Event> onEvent(event: KClass<E>): Flow<E> =
        bot.eventDispatcher.on(event.java).asFlow()

    inline fun <reified E : Event> onEvent() = onEvent(E::class)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun Flow<*>.register(): Job {
        val logger = callerLogger()
        return retry { err ->
            logger.warn("Error in EventFlow, re-collecting", err)
            true
        }
            .launchIn(scope)
    }
}
