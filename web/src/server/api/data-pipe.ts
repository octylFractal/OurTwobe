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
import {Writeable} from "../../utils";

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
        readonly observable:  Observable<DataPipeEvent | DataPipeError>,
    ) {
    }
}

function decodeMessage(m: MessageEvent): DataPipeEvent | DataPipeError {
    let json: Writeable<DataPipeEvent | {type: "some string that will never actually be sent as a type"}>;
    try {
        json = JSON.parse(m.data);
    } catch (e) {
        return {
            error: true,
            value: e,
        };
    }
    // hack the type on
    (json as {type: string}).type = m.type;
    switch (json.type) {
        case 'guildSettings':
        case 'queueItem':
        case 'progressItem':
            return json;
        default:
            return {
                error: true,
                value: new Error(`Unknown type: ${json.type}`),
            };
    }
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
                `${window.location.origin}/guilds/${guildId}/data-pipe`
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
            source.onmessage = (m): void => {
                const message = decodeMessage(m);
                subscriber.next(message);
            };
        } catch (e) {
            subscriber.error(e);
        }
    });
    return new DataPipe(observable);
}
