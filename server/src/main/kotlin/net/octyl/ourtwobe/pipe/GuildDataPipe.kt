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
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.util.Image
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.guild.GuildUpdateEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import net.octyl.ourtwobe.GuildData
import net.octyl.ourtwobe.convertToMapViaJackson
import net.octyl.ourtwobe.initialGuildCreateFlow

class GuildDataImpl(
    override val id: String,
    override val name: String,
    override val iconUrl: String?,
    override val volume: Int = 50,
    override val activeChannel: String? = null,
) : GuildData

@OptIn(ExperimentalCoroutinesApi::class)
class GuildDataPipe(firestore: Firestore) : DiscordEventPipe(firestore, "guilds") {

    private fun Flow<Guild>.sendToFirestore() =
        this
            .map { guild ->
                collection.document(guild.id.asString())
                    .set(
                        GuildDataImpl(
                            guild.id.asString(),
                            guild.name,
                            guild.getIconUrl(Image.Format.PNG).orElse(null),
                        ).convertToMapViaJackson(),
                        SetOptions.merge()
                    )
            }
            .buffer()
            .awaitAllSafe()

    override fun initialPipe(bot: DiscordClient): Flow<*> =
        bot.initialGuildCreateFlow().map { it.guild }.sendToFirestore()

    override fun EventFlowBuilder.registerEventFlows() {
        merge(
            onEvent<GuildCreateEvent>().map { it.guild },
            onEvent<GuildUpdateEvent>().map { it.current }
        ).sendToFirestore().register()
    }
}
