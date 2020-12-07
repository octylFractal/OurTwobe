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

package net.octyl.ourtwobe.datapipe

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import net.octyl.ourtwobe.discord.QueuePlayer

class GuildState(
    private val queuePlayer: QueuePlayer,
    val queueManager: QueueManager,
    val guildSettingsHolder: GuildSettingsHolder,
) {

    suspend fun pumpEventsToPipe(pipe: DataPipe) {
        coroutineScope {
            launch {
                guildSettingsHolder.settings.collect {
                    pipe.sendData(DataPipeEvent.GuildSettings(
                        it.volume,
                        it.activeChannel
                    ))
                }
            }

            launch {
                pipe.sendData(DataPipeEvent.ClearQueues)
                for ((userId, queue) in queueManager.getQueues()) {
                    for (item in queue) {
                        pipe.sendData(DataPipeEvent.QueueItem(
                            userId,
                            item,
                        ))
                    }
                }
                queueManager.events.collect(pipe::sendData)
            }

            launch {
                queuePlayer.events.filterNotNull().collect(pipe::sendData)
            }
        }
    }
}
