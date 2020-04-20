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
import React from "react";
import {DropdownItem, DropdownMenu, DropdownToggle, NavLink, UncontrolledButtonDropdown} from "reactstrap";
import {generateOAuthLink} from "../firebase/auth";
import {firebaseApp} from "../firebase/setup";
import {UserInfoRecord} from "../redux/reducer";
import {NavbarImg} from "./NavbagImg";
import {Redirect} from "react-router-dom";
import DiscordLogo from "../app/Discord-Logo+Wordmark-Color.svg";

function loading() {
    return <div className="navbar-text d-inline-flex align-items-center">
        <FontAwesomeIcon spin icon={faSpinner} size="2x"/>
    </div>;
}

export const UserState: React.FC<{ userInfo: UserInfoRecord }> = ({userInfo: {heardFromFirebase, uid, profile}}) => {
    if (!heardFromFirebase) {
        return loading();
    }
    if (typeof uid === "undefined") {
        // lazily generate OAuth link to prevent overwriting state
        return <NavLink href="#" onClick={() => window.location.assign(generateOAuthLink())}
                        className="d-block">
            {/* ensure that we redirect to the home page on log-out */}
            <Redirect to="/"/>
            <FontAwesomeIcon icon={faSignInAlt}/> Log In to
            <img className="d-inline-block" height={48} src={DiscordLogo} alt="Discord Logo"/>
        </NavLink>;
    }
    if (typeof profile === "undefined") {
        return loading();
    }

    function logOut() {
        firebaseApp.auth().signOut()
            .catch(err => console.error(err));
    }

    return <UncontrolledButtonDropdown>
        <DropdownToggle nav caret>
            <NavbarImg src={profile.avatarUrl} alt="Avatar" className="rounded-circle"/>
            {profile.username}
        </DropdownToggle>
        <DropdownMenu right>
            <DropdownItem onClick={logOut}>
                Log Out
            </DropdownItem>
        </DropdownMenu>
    </UncontrolledButtonDropdown>;
};
