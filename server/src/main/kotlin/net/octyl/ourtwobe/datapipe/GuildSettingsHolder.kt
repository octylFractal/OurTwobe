package net.octyl.ourtwobe.datapipe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GuildSettingsHolder {
    private val _settings = MutableStateFlow(GuildSettings())
    val settings: StateFlow<GuildSettings> = _settings

    fun updateSettings(block: (settings: GuildSettings) -> GuildSettings) {
        var existing: GuildSettings
        var update: GuildSettings
        do {
            existing = _settings.value
            update = block(existing)
        } while (!_settings.compareAndSet(existing, update))
    }
}
