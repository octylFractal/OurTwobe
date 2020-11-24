@file:Export
package net.octyl.ourtwobe.datapipe

import kotlinx.serialization.Serializable
import net.octyl.ourtwobe.interop.Export

@Serializable
data class PlayableItem(
    val youtubeId: String,
    val title: String,
    val thumbnail: Thumbnail,
    val duration: String,
    val id: String,
    val submissionTime: String,
)

@Serializable
data class Thumbnail(
    val width: Int,
    val height: Int,
    val url: String,
)
