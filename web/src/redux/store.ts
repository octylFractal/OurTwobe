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

import {configureStore, Store} from "@reduxjs/toolkit";
import {reducer} from "./reducer";
import {from, InteropObservable} from "rxjs";

export const store = configureStore({
    reducer
});

type StoreToState<T extends Store> = T extends Store<infer R> ? R : never;

export type LocalState = StoreToState<typeof store>;

export const state$ =
    // little hack to work around rxjs types being a pain
    from(store as any as InteropObservable<LocalState>)
