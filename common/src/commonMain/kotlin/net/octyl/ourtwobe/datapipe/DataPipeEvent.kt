@file:Export

package net.octyl.ourtwobe.datapipe

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.octyl.ourtwobe.interop.Export
import kotlin.reflect.typeOf

open class DataPipeEvent {
    companion object {
        // JS is killer :|
        @OptIn(ExperimentalStdlibApi::class)
        val typeMapping = mapOf(
            "guildSettings" to typeOf<GuildSettings>(),
            "queueItem" to typeOf<QueueItem>(),
            "progressItem" to typeOf<ProgressItem>()
        )
        val reverseTypeMapping = typeMapping.asSequence().map { (k, v) -> v to k }.toMap()

        fun deserialize(type: String, data: String) : DataPipeEvent {
            return Json.decodeFromString(
                Json.serializersModule.serializer(typeMapping[type] ?: error("Unknown event type: $type")),
                data
            ) as DataPipeEvent
        }
    }

    @Serializable
    data class GuildSettings(
        val volume: Int,
        val activeChannel: String?,
    ) : DataPipeEvent()

    @Serializable
    data class QueueItem(
        val owner: String,
        val item: PlayableItem,
    ) : DataPipeEvent()

    @Serializable
    data class ProgressItem(
        val item: PlayableItem,
        val progress: Double,
    ) : DataPipeEvent()
}
