interface SecureRandom {

    randomArray(byteCount: number): Array<number>;

    randomUint8Array(byteCount: number): Uint8Array;
}

declare const secureRandom: SecureRandom;

declare module 'secure-random' {
    export = secureRandom
}
