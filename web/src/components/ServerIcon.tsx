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

import {net} from "common";
import React from "react";
import {NavbarImg} from "./NavbagImg";
import Server = net.octyl.ourtwobe.Server;
import classNames from "classnames";

interface ServerIconProps {
    server: Server
    width?: number
    height?: number
    className?: string
}

export const ServerIcon: React.FC<ServerIconProps> = ({server, ...props}) => {
    const width = props.width ?? 48;
    const height = props.height ?? 48;
    const classes = classNames(props.className, "rounded-circle server-icon", {
        "server-icon-text": !server.iconUrl
    });
    if (server.iconUrl) {
        return <NavbarImg className={classes} width={width} height={height} src={server.iconUrl}/>;
    }
    return <span className={classes} style={{
        width, height
    }}>
        {server.name.split(" ").map(it => it[0]).join("")}
    </span>
};
