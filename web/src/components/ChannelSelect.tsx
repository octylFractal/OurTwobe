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

import React, {useEffect, useState} from "react";
import {Input} from "reactstrap";
import {useRandomId} from "./reactHelpers";
import {ChannelId, GuildId} from "../data/DiscordIds";

export interface ChannelSelectProps {
    guildId: GuildId;
}

export const ChannelSelect: React.FC<ChannelSelectProps> = ({guildId}) => {
    const [selectedChannel, setSelectedChannel] = useState("!!");

    useEffect(() => {

    }, [selectedChannel]);

    return <Input type="select" name="channel" bsSize="sm"
                  value={selectedChannel}
                  onChange={e => void setSelectedChannel(e.currentTarget.value)}>
        <option value="!!">None</option>
        {Object.values(channels)
            .sort((a, b) => a.order - b.order)
            .map(channel =>
                <option key={channel.id} value={channel.id}>{channel.name}</option>
            )
        }
    </Input>;
};
