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
import {ROW_ICON_SIZE, RowIcon} from "./RowIcon";
import React, {type ReactElement} from "react";
import {generateOAuthLink} from "../discord/auth";
import {type UserInfoRecord} from "../redux/reducer";
import {NavbarImg} from "./NavbagImg";
import {ListGroup} from "react-bootstrap";
import {getAvatarUrl, getUserNameColor, type User} from "../discord/api/response/User";
import {faSpinner} from "@fortawesome/free-solid-svg-icons/faSpinner";
import {faSignInAlt} from "@fortawesome/free-solid-svg-icons/faSignInAlt";
import {faSignOutAlt} from "@fortawesome/free-solid-svg-icons/faSignOutAlt";

const WORDMARK_URL = new URL("../discord/Discord-Logo-Blurple.svg", import.meta.url).toString();

function loading(): ReactElement {
    return <ListGroup.Item>
        <FontAwesomeIcon spin icon={faSpinner} size="2x"/>
    </ListGroup.Item>;
}

const UserProfileDisplay: React.FC<User> = (props) => {
    const size = ROW_ICON_SIZE;
    return <>
        <NavbarImg src={`${getAvatarUrl(props)}?size=${size}`}
                   alt="Avatar"
                   className="rounded-circle border border-light me-2 flex-shrink-0"
                   width={size}
                   height={size}/>
        <span className="text-break" style={{color: getUserNameColor(props)}}>{props.username}</span>
    </>;
};

export interface UserStateProps {
    userInfo: UserInfoRecord
    logOut: () => void
}

export const UserState: React.FC<UserStateProps> = ({userInfo: {heardFromDiscord, profile}, logOut}) => {
    if (!heardFromDiscord) {
        return loading();
    }
    if (profile === null) {
        // lazily generate OAuth link to prevent overwriting state
        return <ListGroup.Item action href="#" onClick={() => void window.location.assign(generateOAuthLink())}
                               className="d-flex align-items-center">
            <RowIcon icon={faSignInAlt}/>
            <span>
                Log In to
                <img src={WORDMARK_URL} alt="Discord" height={24} className="d-block mt-1"/>
            </span>
        </ListGroup.Item>;
    }

    return <>
        <ListGroup.Item className="d-flex align-items-center">
            <UserProfileDisplay {...profile}/>
        </ListGroup.Item>
        <ListGroup.Item action onClick={logOut} className="d-flex align-items-center">
            {/* the heavy bracket on the left of this icon's glyph makes it look left of center */}
            <RowIcon icon={faSignOutAlt} transform="right-1"/>Log Out
        </ListGroup.Item>
    </>;
};
