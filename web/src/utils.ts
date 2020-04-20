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
