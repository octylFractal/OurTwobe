package net.octyl.ourtwobe.youtube.api

import net.octyl.ourtwobe.datapipe.Thumbnail
import java.time.Duration

/**
 * Un-processed video info from YouTube.
 */
data class Video(
    val id: String,
    val snippet: Snippet,
    val contentDetails: ContentDetails,
) {
    data class Snippet(
        val title: String,
        val thumbnails: Thumbnails
    ) {
        data class Thumbnails(
            val medium: Thumbnail
        )
    }

    data class ContentDetails(
        val duration: Duration,
    )
}
