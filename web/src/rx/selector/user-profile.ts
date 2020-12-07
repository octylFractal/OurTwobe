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

import {observeStore} from "../redux-observable";
import {createSimpleSelector} from "../../redux/selectors";
import {concatMap} from "rxjs/operators";
import {userInfo, userToken} from "../../redux/reducer";
import {logErrorAndRetry} from "../observer";
import {DiscordApi} from "../../discord/api";
import {LocalState} from "../../redux/store";
import {Store} from "redux";
import {AxiosError} from "axios";

export function subscribe(store: Store<LocalState>): void {
    observeStore(store, createSimpleSelector(state => state.userToken))
        .pipe(
            concatMap(async token => {
                if (!token) {
                    store.dispatch(userInfo.clearProfile());
                    return;
                }
                try {
                    const user = await new DiscordApi(token).getMe();
                    store.dispatch(userInfo.loadProfile(user));
                } catch (e) {
                    const axios = e as AxiosError;
                    if ("response" in axios && axios.response?.status === 401) {
                        // we need a new token
                        store.dispatch(userToken.logout());
                        return;
                    }
                    throw e;
                }
            }),
            logErrorAndRetry("user token"),
        )
        .subscribe();
}
