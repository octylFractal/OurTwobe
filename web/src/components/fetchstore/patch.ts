import { useEffect } from "react";
import {useCallback, useState} from "react";

export type FetchStore<Result, Input> = {
    prefetch: (input: Input) => void;
    evict: (input: Input) => void;
    getResult: (input: Input) => Result;
};
type Refetch<Input> = (input: Input) => void;

export function useFetch<Result, Input>(
    store: FetchStore<Result, Input>,
): [undefined, Refetch<Input>];

export function useFetch<Result, Input>(
    store: FetchStore<Result, Input>,
    initialInput: Input,
): [Result, Refetch<Input>];

export function useFetch<Result, Input>(
    store: FetchStore<Result, Input>,
    initialInput?: Input,
): [Result | undefined, Refetch<Input>];

/**
 * Way better implementation of useFetch, not sure why this isn't the default.
 */
export function useFetch<Result, Input>(
    store: FetchStore<Result, Input>,
    initialInput?: Input,
): [Result | undefined, Refetch<Input>] {
    const [result, setResult] = useState(() => {
        if (initialInput === undefined) return undefined;
        store.prefetch(initialInput);
        return store.getResult(initialInput);
    });
    const refetch = useCallback((nextInput: Input) => {
        store.prefetch(nextInput);
        setResult(() => store.getResult(nextInput));
    }, [store]);
    return [result, refetch];
}

export function useAutoFetch<Result, Input>(
    store: FetchStore<Result, Input>,
    input: Input,
): Result {
    const [r, refetch] = useFetch(store, input);
    useEffect(() => {
        refetch(input);
    }, [refetch, input]);
    return r;
}
