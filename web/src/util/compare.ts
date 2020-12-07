export type Comparator<T> = (a: T, b: T) => number;

export class Comparators {
    static readonly NUMBER: Comparator<number> = (a, b) => a < b ? -1 : (a > b ? 1 : 0)

    static comparing<T, K>(keyExtractor: (t: T) => K, comparator: Comparator<K>): Comparator<T> {
        return (a, b): number => comparator(keyExtractor(a), keyExtractor(b));
    }
}
