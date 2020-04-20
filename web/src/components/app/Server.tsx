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
import {net} from "common";
import {useParams} from "react-router-dom";
import {hot} from "react-hot-loader/root";
import {Navbar, NavbarBrand} from "reactstrap";
import {ServerIcon} from "../ServerIcon";
import Server = net.octyl.ourtwobe.Server;

interface ServerProps {
    serverId: string
}

const Server: React.FC<ServerProps> = ({serverId}) => {
    const server = useSelector((state: LocalState) =>
        (state.userInfo.profile?.servers ?? [])
            .find(value => value.id === serverId)
    );
    if (!server) {
        return <></>;
    }
    return <div>
    </div>;
};

const SpecificServer: React.FC = () => {
    const {serverId} = useParams();
    if (typeof serverId === "undefined") {
        return <></>;
    }
    return <Server serverId={serverId}/>;
};

export default hot(SpecificServer);
