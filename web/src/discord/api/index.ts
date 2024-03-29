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

import {type User} from "./response/User";
import {AxiosError, type AxiosRequestConfig} from "axios";
import {type GuildId} from "../../data/DiscordIds";
import {type Channel} from "./response/Channel";
import {type Guild} from "./response/Guild";
import {ApiBase} from "../../util/ApiBase";

const DISCORD_BASE = "https://discord.com/api/v8";
const OURTWOBE_BASE = "/api/discord";

/**
 * A mostly-raw Discord API. Uses some transformations when the raw Discord data is hard to use, or
 * not well-represented in ECMAScript.
 */
export class DiscordApi extends ApiBase {

    constructor(token: string) {
        super(token);
    }

    private async rateLimitedGet<R>(url: string, target: "discord" | "ourtwobe"): Promise<R> {
        const fixedUrl = `${target === "discord" ? DISCORD_BASE : OURTWOBE_BASE}${url}`;
        const conf: AxiosRequestConfig = target === "discord"
            ? {
                headers: {
                    Authorization: `Bearer ${this.token}`,
                },
            }
            : {
                auth: {
                    username: "discord",
                    password: this.token,
                },
            };
        for (let i = 0; i < 5; i++) {
            try {
                return this.doRequest("get", fixedUrl, conf);
            } catch (e) {
                const axios = e as AxiosError;
                if ("response" in axios && axios.response?.status === 429) {
                    // We are being rate limited :)
                    const delay = (axios.response.data as {retry_after: number}).retry_after * 1000;
                    await new Promise(resolve => {
                        setTimeout(resolve, delay);
                    });
                    continue;
                }
                throw e;
            }
        }
        throw new Error(`Failed to retrieve ${url} before running out of retries`);
    }

    getMe(): Promise<User> {
        return this.rateLimitedGet(`/users/@me`, "discord");
    }

    getUser(userId: string): Promise<User> {
        return this.rateLimitedGet<User>(`/users/${userId}`, "ourtwobe");
    }

    getGuilds(): Promise<Guild[]> {
        return this.rateLimitedGet<Guild[]>(`/guilds`, "ourtwobe");
    }

    getGuild(guildId: GuildId): Promise<Guild> {
        return this.rateLimitedGet(`/guilds/${guildId}`, "ourtwobe");
    }

    /**
     * Get the channels in the guild, they will be ordered already.
     *
     * @param guildId the guild to get channels from
     */
    getChannels(guildId: GuildId): Promise<Channel[]> {
        return this.rateLimitedGet(`/guilds/${guildId}/channels`, "ourtwobe");
    }
}
