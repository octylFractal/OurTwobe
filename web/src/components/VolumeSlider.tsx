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
import {Col, Form, OverlayTrigger, Row, Tooltip} from "react-bootstrap";
import {useUniqueId} from "./reactHelpers";
import {asNonNull} from "../utils";
import {usePrevious} from "./hook/usePrevious";

export interface VolumeSliderProps {
    guildId: GuildId
}

export const VolumeSlider: React.FC<VolumeSliderProps> = ({guildId}) => {
    const volId = useUniqueId("volume");
    const volume = useSelector((state: LocalState) => state.guildState[guildId]?.settings?.volume);
    const lastVolume = usePrevious(volume);
    const [userVolume, setUserVolume] = useState(volume);
    const lastUserVolume = usePrevious(userVolume);
    const commApi = useNonNullContext(CommApiContext);

    useEffect(() => {
        if (typeof userVolume === "undefined") {
            // initial state needs to be set
            setUserVolume(volume);
        } else if (lastVolume === userVolume && userVolume === lastUserVolume) {
            // change from server is being propagated to us, accept it
            setUserVolume(volume);
        }
    }, [userVolume, lastUserVolume, volume, lastVolume]);

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

    return <Form.Group as={Row} className="align-items-center" controlId={volId}>
        <Form.Label column>Volume:</Form.Label>
        <Col className="p-0">
            <span>{volume}</span>
        </Col>
        <Col className="d-flex align-items-center">
            {/* Restore a normal value for width */}
            <OverlayTrigger placement="top" overlay={
                <Tooltip>
                    {userVolume}
                </Tooltip>
            }>
                <Form.Range
                    className="w-auto border-success"
                    name="volume"
                    min={0} max={100}
                    value={userVolume || 0}
                    onChange={(e: ChangeEvent<HTMLInputElement>): void => {
                        setUserVolume(e.currentTarget.valueAsNumber);
                    }}
                    onMouseUp={doChange}
                    onTouchEnd={doChange}
                    onBlur={doChange}/>
            </OverlayTrigger>
        </Col>
    </Form.Group>;
};
