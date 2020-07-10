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

import discord4j.core.DiscordClient
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.reactive.asFlow

inline fun <reified E : Event> EventDispatcher.on(): Flow<E> = on(E::class.java).asFlow()

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
fun DiscordClient.initialGuildCreateFlow(): Flow<GuildCreateEvent> =
    eventDispatcher.on<ReadyEvent>()
        .take(1)
        .flatMapConcat { event ->
            eventDispatcher.on<GuildCreateEvent>()
                .take(event.guilds.size)
        }
