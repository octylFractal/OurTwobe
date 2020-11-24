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

import {net} from "common";
import {Observable} from "rxjs";
import DataPipeEvent = net.octyl.ourtwobe.datapipe.DataPipeEvent;


export interface DataPipeError {
    error: true
    value: unknown
}

/**
 * Create a new data-pipe observable.
 *
 * Requires that authentication has already been performed, so there is an active cookie in the browser.
 *
 * @param guildId the guild that the events should come from
 */
function newDataPipe(guildId: string): Observable<DataPipeEvent | DataPipeError> {
    return new Observable(subscriber => {
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
                const message = DataPipeEvent.Companion.deserialize(m.type, m.data);
                subscriber.next(message);
            };
        } catch (e) {
            subscriber.error(e);
        }
    });
}
