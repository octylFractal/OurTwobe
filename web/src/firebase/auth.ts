import firebase from "firebase/app";
import $ from "jquery";
import {LS_CONSTANTS} from "../app/localStorage";
import secureRandom from "secure-random";
import queryString from "query-string";
import {fromByteArray} from "base64-js";
import {net} from "common";
import TokenHolder = net.octyl.ourtwobe.TokenHolder;

export async function finishDiscordLogIn(discordToken: string): Promise<void> {
    await firebase.auth().setPersistence(firebase.auth.Auth.Persistence.LOCAL);
    const response = await $.post({
        url: "/api/login/discord",
        data: JSON.stringify({token: discordToken} as TokenHolder)
    });
    if (response.error) {
        throw Error(JSON.stringify(response));
    }
    const token = response.token;
    firebase.auth().useDeviceLanguage();
    await firebase.auth().signInWithCustomToken(token);
}

const baseUrl = 'https://discordapp.com/api/oauth2/authorize';
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
        redirect_uri: redirectUri
    });
}
