@file:Export
package net.octyl.ourtwobe

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.octyl.ourtwobe.interop.Export
import net.octyl.ourtwobe.interop.JsonCodec

@Serializable
data class QueueSubmit(
    val url: String,
) {
    companion object : JsonCodec<QueueSubmit> {
        override fun encode(value: QueueSubmit): String = Json.encodeToString(value)

        override fun decode(value: String): QueueSubmit = Json.decodeFromString(value)
    }
}
