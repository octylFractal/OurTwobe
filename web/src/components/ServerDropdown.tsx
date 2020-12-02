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

import {Dropdown, NavDropdown, NavItem, NavLink} from "react-bootstrap";
import React from "react";
import {RoutedDropdownLink} from "./compat/RoutedDropdownLink";
import {ServerIcon} from "./ServerIcon";
import {Guild} from "../discord/api/response/Guild";

export interface ServerDropdownProps {
    guilds: Guild[]
}

export const ServerDropdown: React.FC<ServerDropdownProps> = ({guilds}) => {
    return <Dropdown as={NavItem}>
        <Dropdown.Toggle as={NavLink} disabled={guilds.length === 0}>Server</Dropdown.Toggle>
        <Dropdown.Menu className="server-dropdown">
            {guilds.length === 0
                ? <NavDropdown.Item disabled>No servers.</NavDropdown.Item>
                : guilds.map(server =>
                    <RoutedDropdownLink
                        to={`/server/${server.id}`}
                        key={server.id}>
                        <ServerIcon guildData={server} className="mr-3"/>
                        {server.name}
                    </RoutedDropdownLink>
                )}
        </Dropdown.Menu>
    </Dropdown>;
};
