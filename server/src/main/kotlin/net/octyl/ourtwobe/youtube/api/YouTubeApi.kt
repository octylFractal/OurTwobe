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

package net.octyl.ourtwobe.youtube.api

import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.path
import io.ktor.http.takeFrom
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import net.octyl.ourtwobe.MODULES
import net.octyl.ourtwobe.util.nagle
import java.time.Duration

class YouTubeApi(token: String) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            jackson {
                registerModules(MODULES)
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            }
        }
        install(DefaultRequest) {
            url {
                takeFrom("https://www.googleapis.com/youtube/v3/")
                parameters["key"] = token
            }
        }
    }

    private suspend inline fun <reified T> getErrorHandled(block: HttpRequestBuilder.() -> Unit): T {
        try {
            return client.get(block = block).body()
        } catch (e: ClientRequestException) {
            val error = e.response.bodyAsText()
            throw IllegalStateException("YouTube API Error: $error", e)
        }
    }

    /**
     * Given a playlist ID, get all video IDs in the playlist.
     */
    fun getPlaylistItems(playlistId: String): Flow<String> {
        return flow {
            var pageToken: String? = null
            do {
                val page = getErrorHandled<YouTubePage<PlaylistItem>> {
                    url {
                        path("playlistItems")
                        parameters["playlistId"] = playlistId
                        parameters["part"] = "contentDetails"
                        parameters["maxResults"] = "50"
                        pageToken?.let {
                            parameters["pageToken"] = it
                        }
                    }
                }
                for (item in page.items) {
                    emit(item.contentDetails.videoId)
                }
                pageToken = page.nextPageToken
            } while (pageToken != null)
        }
    }

    fun getVideoInfo(videoIds: Flow<String>): Flow<VideoInfo> {
        // wait up to 10ms to get video IDs, then execute
        return videoIds.nagle(50, Duration.ofMillis(10))
            .map { ids ->
                val page = getErrorHandled<YouTubePage<Video>> {
                    url {
                        path("videos")
                        parameters.appendAll("id", ids)
                        parameters.appendAll("part", listOf("snippet", "contentDetails"))
                    }
                }
                check(page.nextPageToken == null) {
                    "Next page token exists, it was not a single page of IDs?"
                }
                page.items
            }
            .transform {
                for (item in it) {
                    emit(VideoInfo(
                        item.id,
                        item.snippet.title,
                        item.snippet.thumbnails.medium,
                        item.contentDetails.duration,
                    ))
                }
            }
    }
}
