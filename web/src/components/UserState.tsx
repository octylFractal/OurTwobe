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

import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import React, {type ReactElement} from "react";
import {generateOAuthLink} from "../discord/auth";
import {type UserInfoRecord} from "../redux/reducer";
import {NavbarImg} from "./NavbagImg";
import {Nav, NavDropdown} from "react-bootstrap";
import {useUniqueId} from "./reactHelpers";
import {getAvatarUrl, getUserNameColor, type User} from "../discord/api/response/User";
import {faSpinner} from "@fortawesome/free-solid-svg-icons/faSpinner";
import {faSignInAlt} from "@fortawesome/free-solid-svg-icons/faSignInAlt";

function loading(): ReactElement {
    return <div className="navbar-text d-inline-flex align-items-center">
        <FontAwesomeIcon spin icon={faSpinner} size="2x"/>
    </div>;
}

const UserProfileDisplay: React.FC<User> = (props) => {
    const size = 44;
    return <>
        <NavbarImg src={`${getAvatarUrl(props)}?size=${size}`}
                   alt="Avatar"
                   className="rounded-circle border border-light"
                   style={
                       // use content-box so the border doesn't affect the size of the image
                       {
                           boxSizing: "content-box",
                       }
                   }
                   width={size}
                   height={size}/>
        <span style={{color: getUserNameColor(props)}}>{props.username}</span>
    </>;
};

export interface UserStateProps {
    userInfo: UserInfoRecord
    logOut: () => void
}

export const UserState: React.FC<UserStateProps> = ({userInfo: {heardFromDiscord, profile}, logOut}) => {
    const id = useUniqueId("nav-dropdown");
    if (!heardFromDiscord) {
        return loading();
    }
    if (profile === null) {
        // lazily generate OAuth link to prevent overwriting state
        return <Nav.Link href="#" onClick={() => void window.location.assign(generateOAuthLink())}
                         className="d-block">
            <FontAwesomeIcon icon={faSignInAlt}/> Log In to
            <img className="d-inline-block" height={48}
                 src={new URL("../app/Discord-Logo+Wordmark-Color.svg", import.meta.url).toString()}
                 alt="Discord Logo"/>
        </Nav.Link>;
    }

    return <NavDropdown id={id} title={<UserProfileDisplay {...profile}/>}>
        <NavDropdown.Item onClick={logOut}>
            Log Out
        </NavDropdown.Item>
    </NavDropdown>;
};
