package net.octyl.ourtwobe.youtube.api

import net.octyl.ourtwobe.datapipe.Thumbnail
import java.time.Duration

/**
 * Processed video info from YouTube.
 */
class VideoInfo(
    val id: String,
    val title: String,
    val thumbnail: Thumbnail,
    val duration: Duration,
)
