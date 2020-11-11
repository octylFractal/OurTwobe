package net.octyl.ourtwobe.youtube.api

data class PlaylistItem(
    val contentDetails: ContentDetails
) {
    data class ContentDetails(
        val videoId: String,
    )
}
