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
