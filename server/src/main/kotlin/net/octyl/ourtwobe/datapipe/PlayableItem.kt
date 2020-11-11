package net.octyl.ourtwobe.datapipe

import java.time.Duration
import java.time.Instant
import java.util.UUID

data class PlayableItem(
    val youtubeId: String,
    val title: String,
    val thumbnail: Thumbnail,
    val duration: Duration,
    val id: String = UUID.randomUUID().toString(),
    val submissionTime: Instant = Instant.now(),
) : Comparable<PlayableItem> {
    override fun compareTo(other: PlayableItem) = submissionTime.compareTo(other.submissionTime)
}

data class Thumbnail(
    val width: Int,
    val height: Int,
    val url: String,
)
