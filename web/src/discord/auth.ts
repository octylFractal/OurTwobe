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

import {LS_CONSTANTS} from "../app/localStorage";
import secureRandom from "secure-random";
import queryString from "query-string";
import {fromByteArray} from "base64-js";

const baseUrl = 'https://discord.com/api/oauth2/authorize';
const clientId = '400219515310571520';
const redirectUri = window.location.origin + '/discord/';

export function generateOAuthLink(): string {
    const randomStateArray = secureRandom.randomUint8Array(16);
    const state = fromByteArray(randomStateArray);
    localStorage.setItem(LS_CONSTANTS.DISCORD_AUTH_STATE, state);
    return baseUrl + '?' + queryString.stringify({
        response_type: 'token',
        client_id: clientId,
        state: state,
        scope: 'guilds identify',
        redirect_uri: redirectUri,
    });
}
