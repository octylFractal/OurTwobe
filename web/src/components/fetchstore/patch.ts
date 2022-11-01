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

import { useEffect } from "react";
import {useCallback, useState} from "react";
import { FetchStore } from "react-suspense-fetch";

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
        return store.get(initialInput);
    });
    const refetch = useCallback((nextInput: Input) => {
        store.prefetch(nextInput);
        setResult(() => store.get(nextInput));
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
