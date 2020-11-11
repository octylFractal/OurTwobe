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
