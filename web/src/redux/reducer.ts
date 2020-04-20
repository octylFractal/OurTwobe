import {combineReducers, createSlice, PayloadAction} from "@reduxjs/toolkit";
import {net} from "common";
import UserProfile = net.octyl.ourtwobe.UserProfile;

export interface UserInfoRecord {
    readonly heardFromFirebase: boolean
    readonly uid?: string
    readonly profile?: UserProfile
}

const {actions: userInfoActions, reducer: userInfo} = createSlice({
    name: "userInfo",
    initialState: {
        heardFromFirebase: false
    } as UserInfoRecord,
    reducers: {
        login(state, action: PayloadAction<string>) {
            state.heardFromFirebase = true;
            state.uid = action.payload;
        },
        setProfile(state, action: PayloadAction<UserProfile | undefined>) {
            state.profile = action.payload;
        },
        logout() {
            return {heardFromFirebase: true, servers: []};
        }
    }
});

export {userInfoActions as userInfo};

export const reducer = combineReducers({
    userInfo
});
