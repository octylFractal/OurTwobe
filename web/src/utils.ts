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

export function oKeys<T>(o: T): (keyof T)[] {
    return Object.keys(o) as (keyof T)[];
}

export function asNonNull<T>(val: T): NonNullable<T> {
    requireNonNull(val);
    return val;
}

export function requireNonNull<T>(val: T): asserts val is NonNullable<T> {
    if (val === undefined || val === null) {
        throw new Error(
            `Expected 'val' to be defined, but received ${val}`
        );
    }
}

export function exhaustiveCheck(param: never) {
}
