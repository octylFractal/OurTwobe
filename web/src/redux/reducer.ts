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

import {combineReducers, createSlice, type PayloadAction} from "@reduxjs/toolkit";
import {LS_CONSTANTS} from "../app/localStorage";
import {type GuildId, type UserId} from "../data/DiscordIds";
import {type PlayableItem, type ProgressItem, type QueueItem, type RemoveItem} from "../server/api/data-pipe";
import {produce} from "immer";
import {oKeys} from "../utils";
import {type User} from "../discord/api/response/User";

export interface UserInfoRecord {
    readonly heardFromDiscord: boolean
    readonly profile: User | null
}

const {actions: userInfo, reducer: userInfoSlice} = createSlice({
    name: "userInfo",
    initialState: {
        heardFromDiscord: false,
        profile: null,
    } as UserInfoRecord,
    reducers: {
        loadProfile(_, {payload}: PayloadAction<User>): UserInfoRecord {
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

export interface GuildSettings {
    readonly activeChannel: string | null
    readonly volume?: number
}

export interface Queue {
    readonly items: PlayableItem[]
}

export interface GuildState {
    readonly settings: GuildSettings
    readonly queues: Record<UserId, Queue>
    readonly playing?: ProgressItem
}

type GuildStateMap = Record<GuildId, GuildState>;

type GuildPayloadAction<T> = PayloadAction<T & {guildId: GuildId}>;

function dropGuildId<T>(payload: T & {guildId: GuildId}): T {
    return produce(payload, draft => {
        const fixedDraft = draft as T & {guildId?: GuildId};
        delete fixedDraft.guildId;
    });
}

const {actions: guildState, reducer: guildStateSlice} = createSlice({
    name: "guildState",
    initialState: {} as GuildStateMap,
    reducers: {
        updateSettings(state, {payload}: GuildPayloadAction<GuildSettings>): void {
            state[payload.guildId] = {
                ...state[payload.guildId],
                settings: dropGuildId(payload),
            };
        },
        removeQueuedItem(state, {payload}: GuildPayloadAction<RemoveItem>): void {
            const queue = state[payload.guildId]?.queues?.[payload.owner]?.items || [];
            const index = queue.findIndex(it => it.id == payload.itemId);
            if (index === -1) {
                return;
            }
            queue.splice(index, 1);
        },
        addQueuedItem(state, {payload}: GuildPayloadAction<QueueItem>): void {
            state[payload.guildId] = produce(state[payload.guildId] || {}, draft => {
                draft.queues = produce(draft.queues || {}, queuesDraft => {
                    queuesDraft[payload.owner] = produce(queuesDraft[payload.owner] || {items: []}, queueDraft => {
                        queueDraft.items.push(payload.item);
                    });
                });
            });
        },
        clearQueues(state, {payload}: GuildPayloadAction<unknown>): void {
            const guildState = state[payload.guildId] || {};
            guildState.queues = {};
            guildState.playing = undefined;
        },
        updatePlayingItem(state, {payload}: GuildPayloadAction<ProgressItem>): void {
            if (state[payload.guildId].playing?.item?.id !== payload.item.id) {
                // newly playing, de-queue
                const queues = state[payload.guildId].queues;
                oKeys(queues)
                    .forEach(k => {
                        const items = queues[k]?.items;
                        if (!items) {
                            return;
                        }
                        const index = items.findIndex(it => it.id == payload.item.id);
                        if (index === -1) {
                            return;
                        }
                        items.splice(index, 1);
                    });
            }
            state[payload.guildId] = produce(state[payload.guildId] || {}, draft => {
                draft.playing = payload.progress >= 100 ? undefined : dropGuildId(payload);
            });
        },
    },
});

export {userInfo, userToken, guildState};

export const reducer = combineReducers({
    userInfo: userInfoSlice,
    userToken: userTokenSlice,
    guildState: guildStateSlice,
});
