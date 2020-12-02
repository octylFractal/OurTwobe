import {createFetchStore} from "react-hooks-fetch";
import {DiscordApi} from "../../discord/api";
import {Guild} from "../../discord/api/response/Guild";
import {GuildId} from "../../data/DiscordIds";
import {Channel} from "../../discord/api/response/Channel";
import {FetchStore} from "./patch";

export interface DiscordFetch {
    guilds: FetchStore<Guild[], unknown>
    guild: FetchStore<Guild, GuildId>
    channels: FetchStore<Channel[], GuildId>
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
    };
}
