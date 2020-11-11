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

import com.google.common.collect.HashBasedTable
import mu.KotlinLogging
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.AnnotatedEventManager
import net.dv8tion.jda.api.hooks.IEventManager
import net.dv8tion.jda.api.hooks.SubscribeEvent
import java.lang.invoke.MethodHandles
import java.lang.reflect.Modifier
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * [AnnotatedEventManager] but more optimized.
 */
class OptimizedAnnotatedEventManager : IEventManager {
    private val logger = KotlinLogging.logger { }
    private val lock = ReentrantReadWriteLock()
    private val registeredListeners = HashSet<Any>()
    private val eventTable = HashBasedTable.create<Class<*>, Any, (event: GenericEvent) -> Unit>()

    override fun register(listener: Any) {
        lock.write {
            if (registeredListeners.add(listener)) {
                val lookup = MethodHandles.lookup()
                listener.javaClass.declaredMethods.asSequence()
                    // require public, non-static methods only
                    .filter { Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) }
                    // require annotated
                    .filter { it.isAnnotationPresent(SubscribeEvent::class.java) }
                    // require only one argument, with the event
                    .filter {
                        val params = it.parameterTypes
                        params.size == 1 && GenericEvent::class.java.isAssignableFrom(params[0])
                    }
                    .forEach { eventMethod ->
                        eventMethod.isAccessible = true
                        val methodHandle = lookup.unreflect(eventMethod).bindTo(listener)
                        eventTable[eventMethod.parameterTypes[0], listener] = { methodHandle.invoke(it) }
                    }
            }
        }
    }

    override fun unregister(listener: Any) {
        lock.write {
            if (registeredListeners.remove(listener)) {
                eventTable.column(listener).clear()
            }
        }
    }

    override fun handle(event: GenericEvent) {
        lock.read {
            var eventClass: Class<*> = event.javaClass
            do {
                for ((listener, method) in eventTable.row(eventClass)) {
                    try {
                        method(event)
                    } catch (t: Throwable) {
                        logger.warn(t) { "Error occurred in listener ${listener.javaClass}"}
                        if (t is Error) {
                            throw t
                        }
                    }
                }

                eventClass = eventClass.superclass
            } while (eventClass != Object::class.java)
        }
    }

    override fun getRegisteredListeners(): List<Any> = lock.read { registeredListeners.toList() }
}
