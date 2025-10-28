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

import {delay, Observable, of, retry} from "rxjs";
import {oKeys, runBlock, type Writeable} from "../../utils";
import {exhaustMap, map, tap} from "rxjs/operators";
import {logErrorAndRetry} from "../../rx/observer";
import {type ChannelId, type UserId} from "../../data/DiscordIds";

export interface DataPipeError {
    error: true
    value: unknown
}

export interface GuildSettings {
    readonly type: 'guildSettings'
    readonly volume: number
    readonly activeChannel: ChannelId | null
}

export interface QueueItem {
    readonly type: 'queueItem'
    readonly owner: UserId
    readonly item: PlayableItem
}

export interface RemoveItem {
    readonly type: 'removeItem'
    readonly owner: UserId
    readonly itemId: string
}

export interface ProgressItem {
    readonly type: 'progressItem'
    readonly item: PlayableItem
    readonly progress: number
}

export const ERROR_THUMBNAIL = {
    url: new URL("../../app/not-playing.png", import.meta.url).toString(),
    width: 320,
    height: 180,
};

export const NO_PLAYING_ITEM: ProgressItem = {
    type: "progressItem",
    item: {
        contentKey: { type: 'nothing' },
        title: "Nothing",
        thumbnail: ERROR_THUMBNAIL,
        duration: 0,
        id: "not any in-use ID",
        submissionTime: 0,
    },
    progress: 0,
};

export interface ClearQueues {
    readonly type: 'clearQueues'
}

// Not part of the general event set, it's internal to the DP comms only
interface KeepAlive {
    readonly type: 'keepAlive'
    readonly expectNextAt: number
}

export type DataPipeEvent = GuildSettings | QueueItem | RemoveItem | ProgressItem | ClearQueues;

const ALL_TYPES: Set<DataPipeEvent["type"]> = runBlock(() => {
    // Force an object with all types present
    const obj: { [T in DataPipeEvent["type"]]: true } = {
        guildSettings: true,
        queueItem: true,
        removeItem: true,
        progressItem: true,
        clearQueues: true,
    };
    // Collect its keys as a set
    return new Set(oKeys(obj));
});

export interface PlayableItem {
    readonly contentKey: ContentKey
    readonly title: string
    readonly thumbnail?: Thumbnail
    readonly duration: number
    readonly id: string
    readonly submissionTime: number
}

export type ContentKey = YouTubeContentKey | FileContentKey | NothingContentKey;

export function describeContentKey(key: ContentKey): string {
    switch (key.type) {
        case 'youtube':
            return `https://youtu.be/${key.youtubeId}`;
        case 'file':
            return `Uploaded File '${key.filename}'`;
        case 'nothing':
            return `No Content`;
    }
}

export interface YouTubeContentKey {
    readonly type: 'youtube'
    readonly youtubeId: string
}

export interface FileContentKey {
    readonly type: 'file'
    readonly filename: string
}

// This is client-side only, to represent "no item"
export interface NothingContentKey {
    readonly type: 'nothing'
}

export interface Thumbnail {
    readonly width: number
    readonly height: number
    readonly url: string
}

export class DataPipe {
    constructor(
        readonly observable: Observable<DataPipeEvent | DataPipeError>,
    ) {
    }
}

function registerOurEventListener<E extends DataPipeEvent | KeepAlive>(
    es: EventSource,
    type: E["type"],
    consumer: (next: E | DataPipeError) => void
): void {
    function decodeMessage(m: MessageEvent): E | DataPipeError {
        try {
            const json = JSON.parse(m.data) as Writeable<E>;
            json.type = type;
            return json;
        } catch (e) {
            return {
                error: true,
                value: e,
            };
        }
    }

    es.addEventListener(type, (e: Event) => {
        if (!(e instanceof MessageEvent)) {
            throw new Error("This is quite wrong.");
        }
        const message = decodeMessage(e);
        consumer(message);
    });
}

function attachKeepAliveWatcher(source: EventSource, killDataPipe: () => void): void {
    let keepAliveTimer: number | undefined = undefined;
    registerOurEventListener<KeepAlive>(source, 'keepAlive', m => {
        if ("error" in m) {
            console.error("Error getting timeout", m.value);
            return;
        }
        clearTimeout(keepAliveTimer);
        keepAliveTimer = window.setTimeout(killDataPipe, m.expectNextAt - Date.now());
    });
}

const MANUAL_RESET = "Manually resetting pipe due to Keep Alive miss";

/**
 * Create a new data-pipe observable.
 *
 * Requires that authentication has already been performed, so there is an active cookie in the browser.
 *
 * @param guildId the guild that the events should come from
 * @param authenticate function to run authentication if needed
 */
export function newDataPipe(guildId: string, authenticate: () => Promise<void>): DataPipe {
    const observable = new Observable<DataPipeEvent | DataPipeError>(subscriber => {
        try {
            const source = new EventSource(
                `${window.location.origin}/api/guilds/${guildId}/data-pipe`
            );
            source.onerror = (e): void => {
                e.preventDefault();
                if (source.readyState === EventSource.CLOSED) {
                    // hard failure
                    subscriber.error(e);
                } else {
                    // soft failure, re-auth in place
                    authenticate()
                        .catch(err => console.error("Failed to re-authenticate, server down?", err));
                }
            };
            for (const type of ALL_TYPES) {
                registerOurEventListener<DataPipeEvent>(source, type, m => void subscriber.next(m));
            }

            attachKeepAliveWatcher(source, () => {
                console.log("Assuming data-pipe connection lost!");
                source.close();
                subscriber.error(MANUAL_RESET);
            });
            return (): void => {
                source.close();
            };
        } catch (e) {
            subscriber.error(e);
            return undefined;
        }
    });
    return new DataPipe(observable.pipe(
        retry({
            delay: error =>
                of(error).pipe(
                    tap(err => {
                        if (err !== MANUAL_RESET) {
                            console.error("Hard error from data-pipe, re-auth & restart...", err);
                        }
                    }),
                    delay(1000),
                    // Re-authenticate, when it's done retry will occur
                    exhaustMap(() =>
                        of({}).pipe(
                            map(authenticate),
                            logErrorAndRetry("authentication")
                        )
                    ),
                )
        })
    ));
}
