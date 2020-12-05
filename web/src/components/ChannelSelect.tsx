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

import React, {ChangeEvent} from "react";
import {Channel} from "../discord/api/response/Channel";
import FormControl from "react-bootstrap/FormControl";
import {useSelector} from "react-redux";
import {LocalState} from "../redux/store";
import {ChannelId, GuildId} from "../data/DiscordIds";
import {CommApiContext} from "./CommApiContext";
import {useNonNullContext} from "./hook/useNonNullContext";
import {optionalFrom} from "../server/api/communication";
import {useUniqueId} from "./reactHelpers";
import {Form} from "react-bootstrap";

export interface ChannelSelectProps {
    guildId: GuildId
    channels: Channel[]
}

const NONE = "!!";

export const ChannelSelect: React.FC<ChannelSelectProps> = ({guildId, channels}) => {
    const channelControlId = useUniqueId("channel");
    const selectedChannel = useSelector((state: LocalState) => state.guildState[guildId]?.activeChannel);
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

    return <Form.Group controlId={channelControlId}>
        <Form.Label className="mr-2">Channel:</Form.Label>
        <FormControl as="select" name="channel" size="sm"
                     custom
                     value={selectedChannel || NONE}
                     onChange={(e: ChangeEvent<HTMLInputElement>): void => {
                         e.preventDefault();
                         const value = e.currentTarget.value;
                         // I have to reassign this apparently
                         e.currentTarget.value = selectedChannel || NONE;
                         setSelectedChannel(value === NONE ? undefined : value);
                     }}>
            <option value={NONE}>None</option>
            <React.Suspense fallback={<></>}>
                {channels
                    .map(channel =>
                        <option key={channel.id} value={channel.id}>{channel.name}</option>
                    )
                }
            </React.Suspense>
        </FormControl>
    </Form.Group>;
};
