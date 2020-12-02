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
import {store} from "../../redux/store";
import {createSimpleSelector} from "../../redux/selectors";
import {concatMap} from "rxjs/operators";
import {userInfo} from "../../redux/reducer";
import {logErrorAndRetry} from "../observer";
import {User} from "../../discord/api/response/User";
import {DiscordApi} from "../../discord/api";

export function subscribe(): void {
    observeStore(store, createSimpleSelector(state => state.userToken))
        .pipe(
            concatMap(async token => {
                if (!token) {
                    store.dispatch(userInfo.clearProfile());
                    return;
                }
                const user = await new DiscordApi(token).getMe();
                store.dispatch(userInfo.loadProfile({
                    id: user.id,
                    username: user.username,
                    avatarUrl: getAvatarUrl(user),
                }));
            }),
            logErrorAndRetry("user token"),
        )
        .subscribe();
}

function getAvatarUrl(user: User): string {
    if (user.avatar) {
        const ext = user.avatar.startsWith("a_") ? "gif" : "png";
        return `https://cdn.discordapp.com/avatars/${user.id}/${user.avatar}.${ext}`;
    }
    return `https://cdn.discordapp.com/embed/avatars/${parseInt(user.discriminator) % 5}.png`;
}
