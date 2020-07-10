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
import React, {useEffect, useMemo, useState} from "react";
import {RoutedDropdownLink} from "./compat/RoutedDropdownLink";
import classNames from "classnames";
import {net} from "common";
import {ServerIcon} from "./ServerIcon";
import {GuildId, UserId} from "../data/DiscordIds";
import {firebaseApp} from "../firebase/setup";
import {attachFirebase} from "../firebase/pipes";
import GuildData = net.octyl.ourtwobe.GuildData;

export interface SeriesDropdownProps {
    uid?: UserId;
}

export const ServerDropdown: React.FC<SeriesDropdownProps> = ({uid}) => {
    const [open, setOpen] = useState(false);
    const toggle = () => void setOpen(prevState => !prevState);
    const [guilds, setGuilds] = useState<Record<GuildId, GuildData>>({});
    const guildArray = useMemo(() => {
        return Object.values(guilds).sort((a, b) => a.name.localeCompare(b.name));
    }, [guilds]);

    useEffect(() => {
        if (typeof uid === "undefined") {
            return;
        }

        const guildsByUser = firebaseApp.firestore()
            .collectionGroup("users")
            .where("id", "==", uid);

        return attachFirebase(
            `guilds of user ${uid}`,
            guildsByUser,
            data => data.id,
            setGuilds,
        );
    }, [uid]);

    if (typeof uid === "undefined") {
        return <></>;
    }

    const classes = classNames({
        disabled: guildArray.length === 0
    });

    return <Dropdown isOpen={open} toggle={toggle} nav inNavbar>
        <DropdownToggle nav caret className={classes}>
            Server
        </DropdownToggle>
        <DropdownMenu>
            {guildArray.length === 0
                ? <DropdownItem disabled>No servers.</DropdownItem>
                : guildArray.map(server =>
                    <RoutedDropdownLink
                        to={`/server/${server.id}`}
                        key={server.id}>
                        <ServerIcon guildData={server} className="mr-3"/>
                        {server.name}
                    </RoutedDropdownLink>
                )}
        </DropdownMenu>
    </Dropdown>;
};
