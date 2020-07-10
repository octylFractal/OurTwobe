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

import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.VoiceChannel
import discord4j.core.event.domain.channel.VoiceChannelCreateEvent
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent
import discord4j.core.event.domain.channel.VoiceChannelUpdateEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import net.octyl.ourtwobe.VoiceChannelData
import net.octyl.ourtwobe.convertToMapViaJackson
import net.octyl.ourtwobe.initialGuildCreateFlow

class VoiceChannelDataImpl(
    override val id: String,
    override val name: String,
    override val order: Int,
) : VoiceChannelData

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class VoiceChannelDataPipe(firestore: Firestore) : DiscordEventPipe(firestore, "guilds") {

    private fun channelRef(channel: VoiceChannel): DocumentReference {
        return collection.document(channel.guildId.asString())
            .collection("channels")
            .document(channel.id.asString())
    }

    private fun Flow<VoiceChannel>.sendToFirestore() =
        this
            .map { channel ->
                channelRef(channel)
                    .set(
                        VoiceChannelDataImpl(
                            channel.id.asString(),
                            channel.name,
                            channel.position.awaitSingle(),
                        ).convertToMapViaJackson(),
                        SetOptions.merge()
                    )
            }
            .buffer()
            .awaitAllSafe()

    override fun initialPipe(bot: DiscordClient): Flow<*> =
        bot.initialGuildCreateFlow()
            .flatMapConcat { it.guild.channels.asFlow() }
            .filterIsInstance<VoiceChannel>()
            .sendToFirestore()

    override fun EventFlowBuilder.registerEventFlows() {
        merge(
            onEvent<VoiceChannelCreateEvent>().map { it.channel },
            onEvent<VoiceChannelUpdateEvent>().map { it.current },
        ).sendToFirestore().register()
        onEvent<VoiceChannelDeleteEvent>().map { it.channel }
            .map {
                channelRef(it).delete()
            }
            .buffer()
            .awaitAllSafe()
            .register()
    }
}
