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

import React from "react";
import {useSelector} from "react-redux";
import {LocalState} from "../../redux/store";
import {useParams} from "react-router-dom";
import {hot} from "react-hot-loader/root";
import {Navbar, NavbarBrand} from "reactstrap";
import {ServerIcon} from "../ServerIcon";

interface ServerNavbarProps {
    serverId: string
}

const ServerNavbar: React.FC<ServerNavbarProps> = ({serverId}) => {
    const server = useSelector((state: LocalState) =>
        (state.userInfo.profile?.servers ?? [])
            .find(value => value.id === serverId)
    );
    if (!server) {
        return <></>;
    }
    return <Navbar color="dark" dark expand="md">
        <NavbarBrand className="py-1">
            <span className="ourtwobe-at-server">
                <h4 className="font-family-audiowide d-inline align-middle">{' @ '}</h4>
                <ServerIcon server={server} className="mr-3" width={32} height={32}/>
                <span className="font-family-audiowide text-wrap">{server.name}</span>
            </span>
        </NavbarBrand>
    </Navbar>;
};

const SpecificServerNavbar: React.FC = () => {
    const {serverId} = useParams();
    if (typeof serverId === "undefined") {
        return <></>;
    }
    return <ServerNavbar serverId={serverId}/>;
};

export default hot(SpecificServerNavbar);
