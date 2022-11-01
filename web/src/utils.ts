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

export function oKeys<T extends Record<string, unknown>>(o: T): (keyof T)[] {
    return Object.keys(o) as (keyof T)[];
}

export function requireNonNull<T>(val: T, message?: string): asserts val is NonNullable<T> {
    if (val === undefined || val === null) {
        throw new Error(
            message || `Expected 'val' to be defined, but received ${val}`
        );
    }
}

export function asNonNull<T>(val: T): NonNullable<T> {
    requireNonNull(val);
    return val;
}

export function exhaustiveCheck(param: never): void {
    return param;
}

export function runBlock<R>(block: () => R): R {
    return block();
}

export type Writeable<T> = { -readonly [P in keyof T]: T[P] };
