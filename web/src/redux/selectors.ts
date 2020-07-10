import {createSelector} from "@reduxjs/toolkit";

export type Selector<S, R> = ((state: S) => R) & {
    recomputations: () => number;
    resetRecomputations: () => number;
}

export function createSimpleSelector<S, R>(selector: (state: S) => R): Selector<S, R> {
    return createSelector(selector, state => state);
}
