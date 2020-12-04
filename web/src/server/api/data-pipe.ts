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

import {Observable} from "rxjs";
import {oKeys, runBlock, Writeable} from "../../utils";

export interface DataPipeError {
    error: true
    value: unknown
}

export interface GuildSettings {
    readonly type: 'guildSettings'
    readonly volume: number
    readonly activeChannel: string | null
}

export interface QueueItem {
    readonly type: 'queueItem'
    readonly owner: string
    readonly item: PlayableItem
}

export interface ProgressItem {
    readonly type: 'progressItem'
    readonly item: PlayableItem
    readonly progress: number
}

export type DataPipeEvent = GuildSettings | QueueItem | ProgressItem;

const ALL_TYPES: Set<DataPipeEvent["type"]> = runBlock(() => {
    // Force an object with all types present
    const obj: {[T in DataPipeEvent["type"]]: true} = {
        guildSettings: true,
        queueItem: true,
        progressItem: true,
    };
    // Collect its keys as a set
    return new Set(oKeys(obj));
});

export interface PlayableItem {
    readonly youtubeId: string
    readonly title: string
    readonly thumbnail: Thumbnail
    readonly duration: string
    readonly id: string
    readonly submissionTime: string
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

function registerOurEventListener(
    es: EventSource,
    type: DataPipeEvent["type"],
    consumer: (next: DataPipeEvent | DataPipeError) => void
): void {
    function decodeMessage(m: MessageEvent): DataPipeEvent | DataPipeError {
        try {
            const json = JSON.parse(m.data) as Writeable<DataPipeEvent>;
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

/**
 * Create a new data-pipe observable.
 *
 * Requires that authentication has already been performed, so there is an active cookie in the browser.
 *
 * @param guildId the guild that the events should come from
 */
export function newDataPipe(guildId: string): DataPipe {
    const observable = new Observable<DataPipeEvent | DataPipeError>(subscriber => {
        try {
            const source = new EventSource(
                `${window.location.origin}/api/guilds/${guildId}/data-pipe`
            );
            source.onerror = (e): void => {
                if (source.readyState === EventSource.CLOSED) {
                    // hard failure
                    subscriber.error(e);
                } else {
                    // soft failure, materialize error
                    subscriber.next({
                        error: true,
                        value: e,
                    });
                }
            };
            for (const type of ALL_TYPES) {
                registerOurEventListener(source, type, m => void subscriber.next(m));
            }
            return (): void => {
                source.close();
            };
        } catch (e) {
            subscriber.error(e);
            return undefined;
        }
    });
    return new DataPipe(observable);
}
