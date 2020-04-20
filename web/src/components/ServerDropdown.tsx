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

import {Dropdown, DropdownItem, DropdownMenu, DropdownToggle} from "reactstrap";
import React, {useState} from "react";
import {RoutedDropdownLink} from "./compat/RoutedDropdownLink";
import classNames from "classnames";
import {net} from "common";
import {NavbarImg} from "./NavbagImg";
import Server = net.octyl.ourtwobe.Server;
import {ServerIcon} from "./ServerIcon";

export interface SeriesDropdownProps {
    loggedIn: boolean
    servers: Server[]
}

export const ServerDropdown: React.FC<SeriesDropdownProps> = ({loggedIn, servers}) => {
    if (!loggedIn) {
        return <></>;
    }

    let [open, setOpen] = useState(false);
    const toggle = () => setOpen(prevState => !prevState);

    const classes = classNames({
        disabled: servers.length === 0
    });

    return <Dropdown isOpen={open} toggle={toggle} nav inNavbar>
        <DropdownToggle nav caret className={classes}>
            Server
        </DropdownToggle>
        <DropdownMenu>
            {servers.length === 0
                ? <DropdownItem disabled>No servers.</DropdownItem>
                : servers.map(server =>
                    <RoutedDropdownLink
                        to={`/server/${server.id}`}
                        key={server.id}>
                        <ServerIcon server={server} className="mr-3"/>
                        {server.name}
                    </RoutedDropdownLink>
                )}
        </DropdownMenu>
    </Dropdown>
};
