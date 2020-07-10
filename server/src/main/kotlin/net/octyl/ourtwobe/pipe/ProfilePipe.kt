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

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.UserUpdateEvent
import discord4j.core.event.domain.guild.MemberJoinEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import net.octyl.ourtwobe.UserProfile
import net.octyl.ourtwobe.convertToMapViaJackson
import net.octyl.ourtwobe.initialGuildCreateFlow

class UserProfileImpl(
    override val id: String,
    override val username: String,
    override val avatarUrl: String,
) : UserProfile

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ProfilePipe(firestore: Firestore) : DiscordEventPipe(firestore, "profiles") {

    private fun Flow<User>.sendToFirestore() =
        this
            .map { user ->
                collection.document(user.id.asString())
                    .set(UserProfileImpl(
                        user.id.asString(),
                        user.username,
                        user.avatarUrl,
                    ).convertToMapViaJackson(), SetOptions.merge())
            }
            .buffer()
            .awaitAllSafe()

    override fun initialPipe(bot: DiscordClient): Flow<*> =
        bot.initialGuildCreateFlow()
            .flatMapConcat { it.guild.members.asFlow() }
            .sendToFirestore()

    override fun EventFlowBuilder.registerEventFlows() {
        onEvent<MemberJoinEvent>()
            .map { it.member }
            .sendToFirestore()
            .register()
        onEvent<UserUpdateEvent>()
            .map { it.current }
            .sendToFirestore()
            .register()
    }
}
