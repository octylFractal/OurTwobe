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

import React, {useCallback} from "react";
import {useDispatch, useSelector} from "react-redux";
import {type LocalState} from "../redux/store";
import {UserState} from "./UserState";
import {userToken} from "../redux/reducer";

export const LocalUserState: React.FC = () => {
    const userInfo = useSelector((state: LocalState) => state.userInfo);
    const dispatch = useDispatch();
    const logOut = useCallback(() => void dispatch(userToken.logout()), [dispatch]);
    return <UserState userInfo={userInfo} logOut={logOut}/>;
};
