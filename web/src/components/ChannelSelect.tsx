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
import {type Channel} from "../discord/api/response/Channel";
import {useSelector} from "react-redux";
import {type LocalState} from "../redux/store";
import {type ChannelId, type GuildId} from "../data/DiscordIds";
import {CommApiContext} from "./CommApiContext";
import {useNonNullContext} from "./hook/useNonNullContext";
import {optionalFrom} from "../server/api/communication";
import {useUniqueId} from "./reactHelpers";
import {Dropdown} from "react-bootstrap";
import {DiscordApiContext} from "./DiscordApiContext";
import {asNonNull} from "../utils";
import {useAutoFetch} from "./fetchstore/patch";

export interface ChannelSelectProps {
    guildId: GuildId
    channels: Channel[]
}

const NONE = "!!";

export const ChannelSelect: React.FC<ChannelSelectProps> = ({guildId, channels}) => {
    const channelToggleId = useUniqueId("channel");
    const selectedChannel = useSelector((state: LocalState) => state.guildState[guildId]?.settings?.activeChannel);
    const commApi = useNonNullContext(CommApiContext);

    function setSelectedChannel(channel: ChannelId | undefined): void {
        commApi.updateGuildSettings({
            activeChannel: optionalFrom(channel),
        })
            .catch(err =>
                // This should really be more visible, but I can't be arsed right now
                console.error(err)
            );
    }

    const selectedName = channels.find(channel => channel.id === selectedChannel)?.name ?? "None";

    return <div className="d-flex align-items-center gap-2">
        <span>Channel:</span>
        <Dropdown
            onSelect={(value: string | null): void => {
                setSelectedChannel(value === null || value === NONE ? undefined : value);
            }}
        >
            <Dropdown.Toggle id={channelToggleId} variant="secondary" size="sm">{selectedName}</Dropdown.Toggle>
            <Dropdown.Menu>
                <Dropdown.Item eventKey={NONE} active={!selectedChannel}>None</Dropdown.Item>
                {channels
                    .map(channel =>
                        <Dropdown.Item key={channel.id} eventKey={channel.id} active={channel.id === selectedChannel}>
                            {channel.name}
                        </Dropdown.Item>
                    )
                }
            </Dropdown.Menu>
        </Dropdown>
    </div>;
};

export interface LocalChannelSelectProps {
    guildId: GuildId
}

export const LocalChannelSelect: React.FC<LocalChannelSelectProps> = ({guildId}) => {
    const discordApi = asNonNull(use(DiscordApiContext));
    const channels = useAutoFetch(discordApi.fetch.channels, guildId);
    return <ChannelSelect guildId={guildId} channels={channels}/>;
};
