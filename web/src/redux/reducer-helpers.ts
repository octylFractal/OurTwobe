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

import {PayloadAction} from "@reduxjs/toolkit";

type NestedRecord<K1 extends keyof never, K2 extends keyof never, V> = Record<K1, Record<K2, V>>;

export interface NestedRecordAdder<K1 extends keyof never, K2 extends keyof never, I, V = I> {
    (root: NestedRecord<K1, K2, V>, intermediate: PayloadAction<I>): void;
}

export function nestedRecordAdd<K1 extends keyof never, K2 extends keyof never, I, V = I>(
    key1Extractor: (intermediate: I) => K1,
    key2Extractor: (intermediate: I) => K2,
    valueExtractor: (intermediate: I) => V = (value: I): V => value as unknown as V
): NestedRecordAdder<K1, K2, I, V> {
    return (root, {payload: intermediate}): void => {
        const key1 = key1Extractor(intermediate);
        let nested = root[key1];
        if (typeof nested === "undefined") {
            nested = root[key1] = {} as Record<K2, V>;
        }
        nested[key2Extractor(intermediate)] = valueExtractor(intermediate);
    };
}

export function nestedSetAdd<K1 extends keyof never, K2 extends keyof never, I>(
    key1Extractor: (intermediate: I) => K1,
    key2Extractor: (intermediate: I) => K2,
): NestedRecordAdder<K1, K2, I, boolean> {
    return nestedRecordAdd(key1Extractor, key2Extractor,
        // cast to ensure correct generic
        () => true as boolean);
}
