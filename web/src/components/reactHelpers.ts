import {useRef} from "react";

export function useRandomId() {
    return useRef(() => Math.random());
}
