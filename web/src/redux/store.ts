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
