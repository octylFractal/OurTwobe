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

import {collectionChanges, collectionData} from "rxfire/firestore";
import {concatMap, map, tap} from "rxjs/operators";
import produce, {castDraft} from "immer";
import {logErrorAndRetry} from "../rx/observer";
import {firestore} from "firebase/app";
import {Dispatch, SetStateAction} from "react";

export function attachFirebase<V>(
    typeDesc: string,
    collection: firestore.Query,
    keyExtractor: (data: V) => string,
    setDataMap: Dispatch<SetStateAction<Record<string, V>>>
): () => void {
    const handle = collectionData<V>(collection)
        .pipe(
            concatMap(data => data),
            tap(data =>
                setDataMap(dataMap => produce(dataMap, draft => {
                    draft[keyExtractor(data)] = castDraft(data);
                }))
            ),
            logErrorAndRetry(`${typeDesc} additions`)
        )
        .subscribe();
    const handle2 = collectionChanges(collection, ['removed'])
        .pipe(
            concatMap(snapshots => snapshots),
            map(snapshot => keyExtractor(snapshot.doc.data() as V)),
            tap(data =>
                setDataMap(dataMap => produce(dataMap, draft => {
                    delete draft[data];
                }))
            ),
            logErrorAndRetry(`${typeDesc} removals`)
        )
        .subscribe();

    return (): void => {
        handle.unsubscribe();
        handle2.unsubscribe();
    };
}
