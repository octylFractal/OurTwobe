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
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.guild.MemberJoinEvent
import discord4j.core.event.domain.guild.MemberLeaveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import net.octyl.ourtwobe.initialGuildCreateFlow
import net.octyl.ourtwobe.setValue

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class GuildUsersPipe(firestore: Firestore) : DiscordEventPipe(firestore, "guilds") {

    private fun userRef(memberLite: MemberLite): DocumentReference {
        return collection.document(memberLite.guildId.asString())
            .collection("users")
            .document(memberLite.id.asString())
    }

    private fun Flow<MemberLite>.sendToFirestore() =
        this
            .map { member ->
                userRef(member).set(setValue(member.id.asString()))
            }
            .buffer()
            .awaitAllSafe()

    override fun initialPipe(bot: DiscordClient): Flow<*> =
        bot.initialGuildCreateFlow()
            .flatMapConcat { it.guild.members.asFlow() }
            .map { MemberLite(it.id, it.guildId) }
            .sendToFirestore()

    override fun EventFlowBuilder.registerEventFlows() {
        onEvent<MemberJoinEvent>()
            .map { MemberLite(it.member.id, it.member.guildId) }
            .sendToFirestore()
            .register()
        onEvent<MemberLeaveEvent>()
            .map { MemberLite(it.user.id, it.guildId) }
            .map {
                userRef(it).delete()
            }
            .buffer()
            .awaitAllSafe()
            .register()
    }
}

private data class MemberLite(
    val id: Snowflake,
    val guildId: Snowflake,
)
