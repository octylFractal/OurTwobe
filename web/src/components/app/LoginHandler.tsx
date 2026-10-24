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

import React, {useEffect, useState} from "react";
import queryString from "query-string";
import {LS_CONSTANTS} from "../../app/localStorage";
import {Navigate} from "react-router";
import {useDispatch, useSelector} from "react-redux";
import {type LocalState} from "../../redux/store";
import {userToken} from "../../redux/reducer";

const LoginHandler: React.FC = () => {
    const loggedIn = useSelector((state: LocalState) => state.userInfo.heardFromDiscord && state.userInfo.profile !== null);
    const dispatch = useDispatch();
    const [hashData] = useState(() => queryString.parse(window.location.hash) as DiscordLoginData);
    const [failed] = useState(() => hashData.state !== localStorage.getItem(LS_CONSTANTS.DISCORD_AUTH_STATE));
    useEffect(() => {
        if (failed) {
            console.warn("Failed to finish OAuth flow, invalid state returned");
            return;
        }
        dispatch(userToken.login(hashData.access_token));
    }, [failed, hashData, dispatch]);

    if (loggedIn) {
        return <Navigate to="/"/>;
    }
    if (failed) {
        return <div>
            Error logging you in. Try again please!
        </div>;
    }
    return <div>
        Please hold, logging you in...
    </div>;
};

export default LoginHandler;

interface DiscordLoginData {
    access_token: string;
    state: string;

    // anything else is still ok
    [k: string]: unknown;
}
