import {DependencyList, useEffect, useRef, useState} from "react";

export interface PromiseHook<T> {
    loading: boolean
    value: T | undefined
}

export interface Loading<T> extends PromiseHook<T>{
    loading: true
    value: undefined
}

export interface Ready<T> extends PromiseHook<T>{
    loading: false
    value: T
}

interface Error<T> extends PromiseHook<T> {
    loading: false
    value: undefined
    error: unknown
}

export type PromiseHookEnum<T> = Loading<T> | Ready<T>;

export function usePromise<T>(
    promiseOrFn: (() => Promise<T>) | Promise<T>,
    deps?: DependencyList,
): PromiseHookEnum<T> {
    const [state, setState] = useState<PromiseHookEnum<T> | Error<T>>({
        loading: false,
        error: null,
        value: undefined
    });
    const isMounted = useRef(false);
    useEffect(() => {
        isMounted.current = true;
        setState({
            loading: true,
            value: undefined
        });
        let promise: Promise<T>;
        if (typeof promiseOrFn === "function") {
            promise = promiseOrFn();
        } else {
            promise = promiseOrFn;
        }

        promise
            .then((value) => {
                if (isMounted.current) {
                    setState({
                        loading: false,
                        value
                    });
                }
            })
            .catch((error) => {
                if (isMounted.current) {
                    setState({
                        loading: false,
                        error,
                        value: undefined
                    });
                }
            });

        return (): void => {
            isMounted.current = false;
        };
        // We are actually ensuring correct behavior, assuming promiseOrFn uses the deps
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [promiseOrFn, ...(deps || [])]);

    if ("error" in state) {
        throw state.error;
    }

    return state;
}
