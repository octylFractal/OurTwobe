import {faSignInAlt, faSpinner} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import React from "react";
import {DropdownItem, DropdownMenu, DropdownToggle, NavLink, UncontrolledButtonDropdown} from "reactstrap";
import {generateOAuthLink} from "../firebase/auth";
import {firebaseApp} from "../firebase/setup";
import {UserInfoRecord} from "../redux/reducer";
import {NavbarImg} from "./NavbagImg";

function avatarUrl(uid: string, avatar: string): string {
    const ext = avatar.startsWith("a_") ? "gif" : "png";
    return `https://cdn.discordapp.com/avatars/${uid}/${avatar}.${ext}`;
}

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
        return <NavLink href="#" onClick={() => window.location.assign(generateOAuthLink())}>
            <FontAwesomeIcon icon={faSignInAlt}/> Sign In
        </NavLink>;
    }
    if (typeof profile === "undefined") {
        return loading();
    }

    function signOut() {
        firebaseApp.auth().signOut()
            .catch(err => console.error(err));
    }

    return <UncontrolledButtonDropdown>
        <DropdownToggle nav caret>
            <NavbarImg src={avatarUrl(uid, profile.avatar)} alt="Avatar" className="rounded-circle"/>
            {profile.username}
        </DropdownToggle>
        <DropdownMenu right>
            <DropdownItem onClick={signOut}>
                Sign Out
            </DropdownItem>
        </DropdownMenu>
    </UncontrolledButtonDropdown>;
};
