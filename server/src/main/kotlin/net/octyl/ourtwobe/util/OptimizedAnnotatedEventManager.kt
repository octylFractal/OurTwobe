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
