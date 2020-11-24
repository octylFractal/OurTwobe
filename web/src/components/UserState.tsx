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

import {faSignInAlt, faSpinner} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import React, {ReactElement} from "react";
import {DropdownItem, DropdownMenu, DropdownToggle, NavLink, UncontrolledButtonDropdown} from "reactstrap";
import {generateOAuthLink} from "../discord/auth";
import {UserInfoRecord, UserProfile} from "../redux/reducer";
import {NavbarImg} from "./NavbagImg";
import {Redirect} from "react-router-dom";
import DiscordLogo from "../app/Discord-Logo+Wordmark-Color.svg";
import {LS_CONSTANTS} from "../app/localStorage";

function loading(): ReactElement {
    return <div className="navbar-text d-inline-flex align-items-center">
        <FontAwesomeIcon spin icon={faSpinner} size="2x"/>
    </div>;
}

const UserProfileDisplay: React.FC<UserProfile> = (props) => {
    return <>
        <NavbarImg src={props.avatarUrl} alt="Avatar" className="rounded-circle"/>
        {props.username}
    </>;
};

export interface UserStateProps {
    userInfo: UserInfoRecord;
}

export const UserState: React.FC<UserStateProps> = ({userInfo: {heardFromDiscord, profile}}) => {
    if (!heardFromDiscord) {
        return loading();
    }
    if (profile === null) {
        // lazily generate OAuth link to prevent overwriting state
        return <NavLink href="#" onClick={() => void window.location.assign(generateOAuthLink())}
                        className="d-block">
            {/* ensure that we redirect to the home page on log-out */}
            <Redirect to="/"/>
            <FontAwesomeIcon icon={faSignInAlt}/> Log In to
            <img className="d-inline-block" height={48} src={DiscordLogo} alt="Discord Logo"/>
        </NavLink>;
    }

    function logOut(): void {
        localStorage.removeItem(LS_CONSTANTS.DISCORD_TOKEN);
    }

    return <UncontrolledButtonDropdown>
        <DropdownToggle nav caret>
            <UserProfileDisplay {...profile}/>
        </DropdownToggle>
        <DropdownMenu right>
            <DropdownItem onClick={logOut}>
                Log Out
            </DropdownItem>
        </DropdownMenu>
    </UncontrolledButtonDropdown>;
};
