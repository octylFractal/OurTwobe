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
import {NavbarImg} from "./NavbagImg";
import classNames from "classnames";
import {type Guild} from "../discord/api/response/Guild";

interface ServerIconProps {
    guildData: Guild;
    width?: number;
    height?: number;
    className?: string;
}

export const ServerIcon: React.FC<ServerIconProps> = ({guildData, ...props}) => {
    const width = props.width ?? 48;
    const height = props.height ?? 48;
    const classes = classNames(props.className, "rounded-circle server-icon", {
        "server-icon-text": !guildData.iconUrl
    });
    if (guildData.iconUrl) {
        return <NavbarImg className={classes} width={width} height={height} src={guildData.iconUrl}/>;
    }
    return <span className={classes} style={{
        width, height
    }}>
        {guildData.name.split(" ").map(it => it[0]).join("")}
    </span>;
};
