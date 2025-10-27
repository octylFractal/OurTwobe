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

package net.octyl.ourtwobe.datapipe

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.google.common.io.BaseEncoding
import java.awt.image.BufferedImage
import java.io.StringWriter
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.imageio.ImageIO

data class PlayableItem(
    val contentKey: ContentKey,
    val title: String,
    val thumbnail: Thumbnail?,
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
) {
    companion object {
        fun fromBufferedImage(image: BufferedImage): Thumbnail {
            val base64 = StringWriter()
            BaseEncoding.base64().encodingStream(base64).use { out ->
                ImageIO.write(image, "png", out)
            }
            return Thumbnail(
                width = image.width,
                height = image.height,
                url = "data:image/png;base64,$base64",
            )
        }
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(YouTubeContentKey::class, name = "youtube"),
    JsonSubTypes.Type(FileContentKey::class, name = "file"),
)
interface ContentKey {
    fun describe(): String
}

data class YouTubeContentKey(
    val videoId: String,
) : ContentKey {
    override fun describe() = "youtu.be/$videoId"
}

class FileContentKey(
    val filename: String,
    @JsonIgnore
    val fileContent: ByteBuffer,
) : ContentKey {
    override fun describe() = "file://$filename"
}
