import React, {useEffect, useState} from "react";
import queryString from "query-string";
import {LS_CONSTANTS} from "../../app/localStorage";
import {finishDiscordLogIn} from "../../firebase/auth";
import {hot} from "react-hot-loader/root";
import {Redirect} from "react-router-dom";
import {connect} from "react-redux";
import {LocalState} from "../../redux/store";

interface LoginHandlerProps {
    loggedIn: boolean
}

const LoginHandler: React.FC<LoginHandlerProps> = ({loggedIn}) => {
    const [failed, setFailed] = useState(false)
    useEffect(() => {
        const hashData = queryString.parse(window.location.hash) as DiscordLoginData;
        const knownState = localStorage.getItem(LS_CONSTANTS.DISCORD_AUTH_STATE);
        if (hashData.state !== knownState) {
            console.warn("Failed to finish OAuth flow, invalid state returned");
            setFailed(true);
            return;
        }
        finishDiscordLogIn(hashData.access_token)
            .catch(err => {
                console.warn("Failed to log in to Firebase using Discord token", err);
                setFailed(true);
            });
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
    </div>
};

export default hot(connect((state: LocalState) => ({
    loggedIn: state.userInfo.heardFromFirebase && typeof state.userInfo.uid !== "undefined"
}))(LoginHandler));

interface DiscordLoginData {
    access_token: string
    state: string

    // anything else is still ok
    [k: string]: any
}
