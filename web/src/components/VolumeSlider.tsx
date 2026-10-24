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

import React, {type ChangeEvent, useCallback, useState} from "react";
import {useSelector} from "react-redux";
import {type LocalState} from "../redux/store";
import {type GuildId} from "../data/DiscordIds";
import {CommApiContext} from "./CommApiContext";
import {useNonNullContext} from "./hook/useNonNullContext";
import {Col, Form, OverlayTrigger, Row, Tooltip} from "react-bootstrap";
import {useUniqueId} from "./reactHelpers";

export interface VolumeSliderProps {
    guildId: GuildId
}

interface LocalVolume {
    value: number
    dragging: boolean
}

export const VolumeSlider: React.FC<VolumeSliderProps> = ({guildId}) => {
    const volId = useUniqueId("volume");
    const remoteVolume = useSelector((state: LocalState) => state.guildState[guildId]?.settings?.volume);
    // the user's slider value; after release, it's kept until the server's volume changes so the slider doesn't jump back
    const [localVolume, setLocalVolume] = useState<LocalVolume | undefined>(undefined);
    const [lastRemoteVolume, setLastRemoteVolume] = useState(remoteVolume);
    const commApi = useNonNullContext(CommApiContext);

    if (remoteVolume !== lastRemoteVolume) {
        setLastRemoteVolume(remoteVolume);
        if (localVolume && !localVolume.dragging) {
            setLocalVolume(undefined);
        }
    }
    const displayedVolume = localVolume?.value ?? remoteVolume;

    const doChange = useCallback((): void => {
        if (!localVolume?.dragging) {
            return;
        }
        setLocalVolume({value: localVolume.value, dragging: false});
        commApi.updateGuildSettings({
            volume: Math.round(localVolume.value),
        })
            .catch(err => {
                // This should really be more visible, but I can't be arsed right now
                console.error(err);
                setLocalVolume(undefined);
            });
    }, [commApi, localVolume]);

    return <Form.Group as={Row} className="align-items-center" controlId={volId}>
        <Form.Label column>Volume:</Form.Label>
        <Col className="p-0">
            <span>{remoteVolume}</span>
        </Col>
        <Col className="d-flex align-items-center">
            {/* Restore a normal value for width */}
            <OverlayTrigger placement="top" overlay={
                <Tooltip>
                    {displayedVolume}
                </Tooltip>
            }>
                <Form.Range
                    className="w-auto border-success"
                    name="volume"
                    min={0} max={100}
                    value={displayedVolume ?? 0}
                    onChange={(e: ChangeEvent<HTMLInputElement>): void => {
                        setLocalVolume({value: e.currentTarget.valueAsNumber, dragging: true});
                    }}
                    onMouseUp={doChange}
                    onTouchEnd={doChange}
                    onBlur={doChange}/>
            </OverlayTrigger>
        </Col>
    </Form.Group>;
};
