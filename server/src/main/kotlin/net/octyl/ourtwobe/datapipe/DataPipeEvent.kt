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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.CaseFormat
import java.time.Instant

sealed class DataPipeEvent {
    @JsonIgnore
    val eventType = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, javaClass.kotlin.simpleName!!)!!

    data class GuildSettings(
        val volume: Int,
        val activeChannel: String?,
    ) : DataPipeEvent()

    data class QueueItem(
        val owner: String,
        val item: PlayableItem,
    ) : DataPipeEvent()

    data class RemoveItem(
        val owner: String,
        val itemId: String,
    ) : DataPipeEvent()

    data class ProgressItem(
        val item: PlayableItem,
        val progress: Double,
    ) : DataPipeEvent()

    /**
     * Clear all queues that are on the client.
     */
    object ClearQueues : DataPipeEvent()

    /**
     * Keep-alive signal.
     *
     * @param expectNextAt expect another one by this time, otherwise connection has died and should be
     *                      re-established
     */
    data class KeepAlive(
        val expectNextAt: Instant
    ) : DataPipeEvent()
}
