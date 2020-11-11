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

package net.octyl.ourtwobe.youtube

import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.octyl.ourtwobe.datapipe.PlayableItem
import net.octyl.ourtwobe.youtube.api.YouTubeApi

class YouTubeItemResolver(
    private val api: YouTubeApi,
) {
    fun resolveItems(url: String): Flow<PlayableItem> {
        val videoIds = flow {
            val parsed = Url(url)
            when (parsed.host) {
                "youtube.com", "www.youtube.com" -> {
                    when (parsed.encodedPath) {
                        "/watch" -> {
                            parsed.parameters["v"]?.let { emit(it) }
                        }
                        "/playlist" -> {
                            parsed.parameters["list"]?.let {
                                emitAll(api.getPlaylistItems(it))
                            }
                        }
                    }
                }
                "youtu.be" -> {
                    parsed.encodedPath.splitToSequence("/")
                        .firstOrNull { it.isNotEmpty() }?.let {
                            emit(it)
                        }
                }
            }
        }
        return api.getVideoInfo(videoIds).map { info ->
            PlayableItem(
                info.id,
                info.title,
                info.thumbnail,
                info.duration,
            )
        }
    }
}
