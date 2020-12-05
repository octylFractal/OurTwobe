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

import React, {ChangeEvent, useCallback, useEffect, useState} from "react";
import {useSelector} from "react-redux";
import {LocalState} from "../redux/store";
import {GuildId} from "../data/DiscordIds";
import {CommApiContext} from "./CommApiContext";
import {useNonNullContext} from "./hook/useNonNullContext";
import {Form} from "react-bootstrap";
import {useUniqueId} from "./reactHelpers";
import {asNonNull} from "../utils";

export interface VolumeSliderProps {
    guildId: GuildId
}

export const VolumeSlider: React.FC<VolumeSliderProps> = ({guildId}) => {
    const volId = useUniqueId("volume");
    const volume = useSelector((state: LocalState) => state.guildState[guildId]?.volume);
    const [userVolume, setUserVolume] = useState(volume);
    const commApi = useNonNullContext(CommApiContext);

    useEffect(() => {
        if (typeof userVolume === "undefined") {
            setUserVolume(volume);
        }
    }, [userVolume, volume]);

    const setVolume = useCallback((newVolume: number): void => {
        commApi.updateGuildSettings({
            volume: Math.round(newVolume),
        })
            .catch(err => {
                // This should really be more visible, but I can't be arsed right now
                console.error(err);
                setUserVolume(volume);
            });
    }, [commApi, volume]);

    const doChange = useCallback(
        () => void setVolume(asNonNull(userVolume)),
        [setVolume, userVolume]
    );

    const volumeWithChangeData = userVolume === volume
        ? userVolume
        : `Changing from ${volume} to ${userVolume}...`;

    return <Form.Group controlId={volId}>
        <Form.Label className="mr-2">Volume:</Form.Label>
        {/* Restore a normal value for width */}
        <Form.Control
            className="w-auto mr-1"
            type="range"
            name="volume"
            min={0} max={100}
            value={userVolume || 0}
            onChange={(e: ChangeEvent<HTMLInputElement>): void => {
                setUserVolume(e.currentTarget.valueAsNumber);
            }}
            onMouseUp={doChange}
            onTouchEnd={doChange}
            onBlur={doChange}/>
        <span>{volumeWithChangeData}</span>
    </Form.Group>;
};
