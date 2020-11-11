package net.octyl.ourtwobe.ffmpeg

import org.bytedeco.ffmpeg.avutil.AVRational

data class Format(
    val channelLayout: Long,
    val sampleFormat: Int,
    val timeBase: AVRational,
    val sampleRate: Int
)
