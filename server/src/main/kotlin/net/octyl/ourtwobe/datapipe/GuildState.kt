package net.octyl.ourtwobe.datapipe

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import net.octyl.ourtwobe.discord.QueuePlayer

class GuildState(
    private val queuePlayer: QueuePlayer,
    val queueManager: QueueManager,
    val guildSettingsHolder: GuildSettingsHolder,
) {

    suspend fun pumpEventsToPipe(pipe: DataPipe) {
        coroutineScope {
            launch {
                guildSettingsHolder.settings.collect {
                    pipe.sendData(DataPipeEvent.GuildSettings(
                        it.volume,
                        it.activeChannel
                    ))
                }
            }

            launch {
                for ((userId, queue) in queueManager.getQueues()) {
                    for (item in queue) {
                        pipe.sendData(DataPipeEvent.QueueItem(
                            userId,
                            item,
                        ))
                    }
                }
                queueManager.events.collect(pipe::sendData)
            }

            launch {
                queuePlayer.events.filterNotNull().collect(pipe::sendData)
            }
        }
    }
}
