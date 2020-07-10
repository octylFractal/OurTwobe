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
import discord4j.core.DiscordClient
import discord4j.core.`object`.VoiceState
import discord4j.core.event.domain.VoiceStateUpdateEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import net.octyl.ourtwobe.batch
import net.octyl.ourtwobe.initialGuildCreateFlow
import net.octyl.ourtwobe.setValue

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class VoiceStatePipe(firestore: Firestore) : DiscordEventPipe(firestore, "guilds") {

    private fun voiceStateRef(voiceState: VoiceState): DocumentReference {
        return collection.document(voiceState.guildId.asString())
            .collection("channels")
            .document(voiceState.channelId.orElseThrow().asString())
            .collection("users")
            .document(voiceState.userId.asString())
    }

    private fun Flow<VoiceStateChange>.sendToFirestore() =
        this
            .map { (to, from) ->
                firestore.batch {
                    if (from != null && from.channelId.isPresent) {
                        voiceStateRef(from).deleteInBatch()
                    }
                    if (to.channelId.isPresent) {
                        voiceStateRef(to).setInBatch(setValue(to.channelId.get().asString()))
                    } else if (from == null) {
                        // cleanup all voice states for this user
                        // certainly expensive, but it's needed
                        collection.document(to.guildId.asString())
                            .collection("channel")
                            .listDocuments()
                            .forEach {
                                it.collection("user")
                                    .document(to.userId.asString())
                                    .deleteInBatch()
                            }
                    }
                }
            }
            .buffer()
            .awaitAllSafe()

    override fun initialPipe(bot: DiscordClient): Flow<*> =
        bot.initialGuildCreateFlow()
            .flatMapConcat { it.guild.voiceStates.asFlow() }
            .map { VoiceStateChange(it) }
            .sendToFirestore()

    override fun EventFlowBuilder.registerEventFlows() {
        onEvent<VoiceStateUpdateEvent>()
            .map { VoiceStateChange(it.current, it.old.orElse(null)) }
            .sendToFirestore()
            .register()
    }
}

private data class VoiceStateChange(
    val to: VoiceState,
    val from: VoiceState? = null,
)
