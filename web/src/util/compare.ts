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

export type Comparator<T> = (a: T, b: T) => number;

export class Comparators {
    static readonly NUMBER: Comparator<number> = (a, b) => a < b ? -1 : (a > b ? 1 : 0)

    static comparing<T, K>(keyExtractor: (t: T) => K, comparator: Comparator<K>): Comparator<T> {
        return (a, b): number => comparator(keyExtractor(a), keyExtractor(b));
    }
}
