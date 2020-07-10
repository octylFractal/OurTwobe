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
