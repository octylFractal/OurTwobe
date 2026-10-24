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

import React, {use} from "react";
import {DiscordApiContext} from "./DiscordApiContext";
import {asNonNull} from "../utils";
import {useAutoFetch} from "./fetchstore/patch";
import RoutedNavLink from "./compat/RoutedNavLink";
import {ServerIcon} from "./ServerIcon";
import {ROW_ICON_SIZE} from "./RowIcon";
import {ListGroup} from "react-bootstrap";

export const LocalServerList: React.FC = () => {
    const discordApi = asNonNull(use(DiscordApiContext));
    const guilds = useAutoFetch(discordApi.fetch.guilds, discordApi.api.unique);
    if (guilds.length === 0) {
        return <ListGroup.Item style={{cursor: "default"}}>No servers.</ListGroup.Item>;
    }
    return <>
        {guilds.map(guild =>
            <RoutedNavLink key={guild.id} to={`/server/${guild.id}`} className="d-flex align-items-center">
                <ServerIcon guildData={guild} size={ROW_ICON_SIZE} className="me-2 flex-shrink-0"/>
                <span className="text-break">{guild.name}</span>
            </RoutedNavLink>
        )}
    </>;
};
