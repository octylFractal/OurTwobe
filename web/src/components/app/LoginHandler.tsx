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
import {finishDiscordLogIn} from "../../discord/auth";
import {hot} from "react-hot-loader/root";
import {Redirect} from "react-router-dom";
import {connect} from "react-redux";
import {LocalState} from "../../redux/store";

interface LoginHandlerProps {
    loggedIn: boolean;
}

const LoginHandler: React.FC<LoginHandlerProps> = ({loggedIn}) => {
    const [failed, setFailed] = useState(false);
    useEffect(() => {
        const hashData = queryString.parse(window.location.hash) as DiscordLoginData;
        const knownState = localStorage.getItem(LS_CONSTANTS.DISCORD_AUTH_STATE);
        if (hashData.state !== knownState) {
            console.warn("Failed to finish OAuth flow, invalid state returned");
            setFailed(true);
            return;
        }
        finishDiscordLogIn(hashData.access_token);
    }, []);

    if (loggedIn) {
        return <Redirect to="/"/>;
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

export default hot(connect((state: LocalState) => ({
    loggedIn: state.userInfo.heardFromDiscord && state.userInfo.profile !== null
}))(LoginHandler));

interface DiscordLoginData {
    access_token: string;
    state: string;

    // anything else is still ok
    [k: string]: unknown;
}
