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

import {combineReducers, createSlice, PayloadAction} from "@reduxjs/toolkit";
import {LS_CONSTANTS} from "../app/localStorage";
import {GuildId, UserId} from "../data/DiscordIds";

export interface UserProfile {
    id: UserId
    username: string
    avatarUrl: string
}

export interface UserInfoRecord {
    readonly heardFromDiscord: boolean
    readonly profile: UserProfile | null
}

const {actions: userInfo, reducer: userInfoSlice} = createSlice({
    name: "userInfo",
    initialState: {
        heardFromDiscord: false,
        profile: null,
    } as UserInfoRecord,
    reducers: {
        loadProfile(_, {payload}: PayloadAction<UserProfile>): UserInfoRecord {
            return {
                heardFromDiscord: true,
                profile: payload,
            };
        },
        clearProfile(): UserInfoRecord {
            return {
                heardFromDiscord: true,
                profile: null,
            };
        },
    }
});

const {actions: userToken, reducer: userTokenSlice} = createSlice({
    name: "userToken",
    initialState: localStorage.getItem(LS_CONSTANTS.DISCORD_TOKEN),
    reducers: {
        login(_, {payload}: PayloadAction<string>): string {
            localStorage.setItem(LS_CONSTANTS.DISCORD_TOKEN, payload);
            return payload;
        },
        logout(): null {
            localStorage.removeItem(LS_CONSTANTS.DISCORD_TOKEN);
            return null;
        },
    }
});

export interface GuildState {
    activeChannel: string | null
    volume?: number
}
type GuildStateMap = Record<GuildId, GuildState>;

const {actions: guildState, reducer: guildStateSlice} = createSlice({
    name: "guildState",
    initialState: {} as GuildStateMap,
    reducers: {
        updateState(state, {payload}: PayloadAction<GuildState & {guildId: GuildId}>): void {
            state[payload.guildId] = {...state[payload.guildId], ...payload};
        },
    },
});

export {userInfo, userToken, guildState};

export const reducer = combineReducers({
    userInfo: userInfoSlice,
    userToken: userTokenSlice,
    guildState: guildStateSlice,
});
