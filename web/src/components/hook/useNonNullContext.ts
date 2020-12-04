import {Context, useContext} from "react";
import {requireNonNull} from "../../utils";

export function useNonNullContext<T>(context: Context<T>) : NonNullable<T> {
    const ctx = useContext(context);
    requireNonNull(ctx, `No context available for ${context.displayName}`);
    return ctx;
}
