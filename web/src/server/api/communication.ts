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

import {type ChannelId} from "../../data/DiscordIds";
import {ApiBase} from "../../util/ApiBase";

/**
 * Communication-only API interface. The data-pipe is a separate class.
 */
export class OurTwobeCommApi extends ApiBase {
    constructor(
        token: string,
        private readonly guildId: string
    ) {
        super(token, {
            baseURL: `${window.location.origin}/api`,
            auth: {
                username: "discord",
                password: token,
            },
        });
    }

    authenticate(): Promise<void> {
        return this.doRequest("get", "/authenticate", {});
    }

    updateGuildSettings(guildUpdate: GuildUpdate): Promise<void> {
        return this.doRequest("put", `/guilds/${this.guildId}`, {data: guildUpdate});
    }

    submitYouTubeItems(item: QueueSubmit): Promise<void> {
        return this.doRequest("post", `/guilds/${this.guildId}/queue/youtube`, {data: item});
    }

    submitFileItems(form: FormData): Promise<void> {
        return this.doRequest("post", `/guilds/${this.guildId}/queue/file`, {data: form});
    }

    removeItem(request: ItemRemove): Promise<void> {
        return this.doRequest("delete", `/guilds/${this.guildId}/queue`, {data: request});
    }

    skipItem(request: ItemSkip): Promise<void> {
        return this.doRequest("post", `/guilds/${this.guildId}/skip`, {data: request});
    }
}

export interface ApiOptional<T> {
    value?: T
}

export function optionalFrom<T>(value: T | undefined): ApiOptional<T> {
    return typeof value === "undefined" ? {} : {value};
}

export interface GuildUpdate {
    volume?: number
    activeChannel?: ApiOptional<ChannelId>
}

export interface QueueSubmit {
    url: string
}

export interface ItemRemove {
    itemId: string
}

export interface ItemSkip {
    itemId: string
}
