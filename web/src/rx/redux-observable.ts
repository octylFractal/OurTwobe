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
import type {SimpleSelector} from "../redux/selectors";

export function observeStore<S, R>(
    store: Store<S>,
    selector: SimpleSelector<S, R>
): Observable<R> {
    return new Observable<R>(subscriber => {
        // Fire initial value as well
        subscriber.next(selector(store.getState()));
        return store.subscribe(() => {
            try {
                const value = selector(store.getState());
                if (selector.recomputations() > 0) {
                    selector.resetRecomputations();
                    subscriber.next(value);
                }
            } catch (e) {
                subscriber.error(e);
            }
        });
    });
}
