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

import React, {useMemo} from "react";
import {DiscordApi} from "../discord/api";
import {useSelector} from "react-redux";
import {type LocalState} from "../redux/store";
import {createFetches, type DiscordFetch} from "./fetchstore/discord";

export interface ApiAndFetch {
    api: DiscordApi
    fetch: DiscordFetch
}

export const DiscordApiContext = React.createContext<ApiAndFetch | null>(null);

export interface DiscordApiProviderProps {
    fallback?: React.ReactNode
}

const apis = new Map<string, ApiAndFetch>();

function getApiAndFetch(userToken: string): ApiAndFetch {
    let apiAndFetch = apis.get(userToken);
    if (typeof apiAndFetch === "undefined") {
        const api = new DiscordApi(userToken);
        apiAndFetch = {api, fetch: createFetches(api)};
        apis.set(userToken, apiAndFetch);
    }
    return apiAndFetch;
}

export const DiscordApiProvider: React.FC<React.PropsWithChildren<DiscordApiProviderProps>> = ({fallback, children}) => {
    const userToken = useSelector((state: LocalState) => state.userToken) || null;
    const discordApi = useMemo(() => userToken ? getApiAndFetch(userToken) : null, [userToken]);
    if (!discordApi) {
        return <>{fallback || "Please log in first."}</>;
    }
    return <DiscordApiContext.Provider value={discordApi}>
        {children}
    </DiscordApiContext.Provider>;
};
