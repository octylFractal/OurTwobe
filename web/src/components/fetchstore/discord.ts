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

import {createFetchStore} from "react-hooks-fetch";
import {DiscordApi} from "../../discord/api";
import {Guild} from "../../discord/api/response/Guild";
import {GuildId, UserId} from "../../data/DiscordIds";
import {Channel} from "../../discord/api/response/Channel";
import {FetchStore} from "./patch";
import {User} from "../../discord/api/response/User";

export interface DiscordFetch {
    guilds: FetchStore<Guild[], unknown>
    guild: FetchStore<Guild, GuildId>
    channels: FetchStore<Channel[], GuildId>
    users: FetchStore<User, UserId>
}

function createWithPrefetch<I, R>(fetchFunc: (input: I) => Promise<R>, initial: I): FetchStore<R, I> {
    const store = createFetchStore(fetchFunc);
    store.prefetch(initial);
    return store;
}

export function createFetches(api: DiscordApi): DiscordFetch {
    return {
        guilds: createWithPrefetch(() => api.getGuilds(), api.unique),
        guild: createFetchStore(guildId => api.getGuild(guildId)),
        channels: createFetchStore(guildId => api.getChannels(guildId)),
        users: createFetchStore(userId => api.getUser(userId)),
    };
}
