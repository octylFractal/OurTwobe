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

import {type Store} from "redux";
import {Observable} from "rxjs";

/**
 * Emit the selected value now, and again each time it changes (compared with `===`).
 */
export function observeStore<S, R>(
    store: Store<S>,
    selector: (state: S) => R
): Observable<R> {
    return new Observable<R>(subscriber => {
        let last = selector(store.getState());
        subscriber.next(last);
        return store.subscribe(() => {
            try {
                const value = selector(store.getState());
                if (value !== last) {
                    last = value;
                    subscriber.next(value);
                }
            } catch (e) {
                subscriber.error(e);
            }
        });
    });
}
