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

import axios, {AxiosInstance} from "axios";
import {ChannelId} from "../../data/DiscordIds";

/**
 * Communication-only API interface. The data-pipe is a separate class.
 */
export class OurTwobeCommApi {
    private readonly client: AxiosInstance;

    constructor(token: string, guildId: string) {
        this.client = axios.create({
            baseURL: `${window.location.origin}/guilds/${guildId}/`,
            auth: {
                username: "communication",
                password: token,
            },
        });
    }

    async updateGuildSettings(guildUpdate: GuildUpdate): Promise<void> {
        return this.client.post("/guilds/", guildUpdate);
    }
}

export interface ApiOptional<T> {
    value?: T
}

export interface GuildUpdate {
    volume?: number
    activeChannel?: ApiOptional<ChannelId>
}
