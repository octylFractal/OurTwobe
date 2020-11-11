package net.octyl.ourtwobe.youtube.api

data class YouTubePage<T>(
    val nextPageToken: String? = null,
    val items: List<T>,
)
