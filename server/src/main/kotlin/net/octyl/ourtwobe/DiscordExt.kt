package net.octyl.ourtwobe

import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.Event
import reactor.core.publisher.Flux

inline fun <reified E : Event> EventDispatcher.on(): Flux<E> = on(E::class.java)
