package net.octyl.ourtwobe.files

import net.octyl.ourtwobe.datapipe.FileContentKey
import net.octyl.ourtwobe.datapipe.PlayableItem
import net.octyl.ourtwobe.ffmpeg.AvioCallbacks
import net.octyl.ourtwobe.ffmpeg.FileItemInfo
import java.nio.ByteBuffer

class FileItemResolver {
    fun resolveItem(filename: String, fileContent: ByteBuffer): PlayableItem {
        val info = FileItemInfo.load(filename, AvioCallbacks.forReadingByteBuffer(fileContent))
        return PlayableItem(
            contentKey = FileContentKey(filename, fileContent),
            title = info.title ?: filename,
            thumbnail = info.thumbnail,
            duration = info.duration,
        )
    }
}
