import React, {useMemo} from "react";
import {DiscordApi} from "../discord/api";
import {useSelector} from "react-redux";
import {LocalState} from "../redux/store";
import {createFetches, DiscordFetch} from "./fetchstore/discord";

export interface ApiAndFetch {
    api: DiscordApi
    fetch: DiscordFetch
}

export const DiscordApiContext = React.createContext<ApiAndFetch | null>(null);

export interface DiscordApiProviderProps {
    fallback?: React.ReactNode
}

const apis = new Map<string, ApiAndFetch>();

export function getApi(userToken: string): DiscordApi {
    return getApiAndFetch(userToken).api;
}

function getApiAndFetch(userToken: string): ApiAndFetch {
    let apiAndFetch = apis.get(userToken);
    if (typeof apiAndFetch === "undefined") {
        const api = new DiscordApi(userToken);
        apiAndFetch = {api, fetch: createFetches(api)};
        apis.set(userToken, apiAndFetch);
    }
    return apiAndFetch;
}

export const DiscordApiProvider: React.FC<DiscordApiProviderProps> = ({fallback, children}) => {
    const userToken = useSelector((state: LocalState) => state.userToken) || null;
    const discordApi = useMemo(() => userToken ? getApiAndFetch(userToken) : null, [userToken]);
    if (!discordApi) {
        return <>{fallback || "Please log in first."}</>;
    }
    return <DiscordApiContext.Provider value={discordApi}>
        {children}
    </DiscordApiContext.Provider>;
};
