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

export interface UserInfoRecord {
    readonly heardFromFirebase: boolean;
    readonly uid?: string;
}

const {actions: userInfo, reducer: userInfoSlice} = createSlice({
    name: "userInfo",
    initialState: {
        heardFromFirebase: false
    } as UserInfoRecord,
    reducers: {
        login(state, {payload}: PayloadAction<string>): void {
            state.heardFromFirebase = true;
            state.uid = payload;
        },
        logout(): UserInfoRecord {
            return {heardFromFirebase: true};
        },
    }
});

export {userInfo};

export const reducer = combineReducers({
    userInfo: userInfoSlice,
});
