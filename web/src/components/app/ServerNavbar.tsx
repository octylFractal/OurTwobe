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

import React, {useContext} from "react";
import {useParams} from "react-router-dom";
import {ServerIcon} from "../ServerIcon";
import {ChannelSelect} from "../ChannelSelect";
import {DiscordApiContext} from "../DiscordApiContext";
import {asNonNull} from "../../utils";
import {type GuildId} from "../../data/DiscordIds";
import {Container, Form, Nav, Navbar} from "react-bootstrap";
import {useUniqueId} from "../reactHelpers";
import {VolumeSlider} from "../VolumeSlider";
import {ItemSubmission} from "../ItemSubmission";
import {useAutoFetch} from "../fetchstore/patch";

interface ServerNavbarProps {
    guildId: GuildId
}

const ServerNavbar: React.FC<ServerNavbarProps> = ({guildId}) => {
    const id = useUniqueId("collapse");
    const discordApi = asNonNull(useContext(DiscordApiContext));
    const guild = useAutoFetch(discordApi.fetch.guild, guildId);
    const channels = useAutoFetch(discordApi.fetch.channels, guildId);

    return <Navbar bg="dark" variant="dark" expand="md">
        <Container fluid>
            <Navbar.Brand className="py-1 border-light me-3">
            <span className="ourtwobe-at-server">
                <h4 className="font-family-audiowide d-inline align-middle">{' @ '}</h4>
                <ServerIcon guildData={guild} className="me-3"/>
                <span className="font-family-audiowide text-wrap align-middle">{guild.name}</span>
            </span>
            </Navbar.Brand>
            <Navbar.Toggle aria-controls={id} className="mx-auto"/>
            <Navbar.Collapse id={id}>
                <Nav className="my-2 align-items-center">
                    <Form className="flex-fill px-3 border-end border-light">
                        <ChannelSelect guildId={guildId} channels={channels}/>
                    </Form>
                    <Form className="flex-fill px-3 border-end border-light">
                        <VolumeSlider guildId={guildId}/>
                    </Form>
                    <ItemSubmission/>
                </Nav>
            </Navbar.Collapse>
        </Container>
    </Navbar>;
};

const SpecificServerNavbar: React.FC = () => {
    const {guildId} = useParams<{ guildId: string }>();
    if (typeof guildId === "undefined") {
        return <></>;
    }
    return <ServerNavbar guildId={guildId}/>;
};

export default SpecificServerNavbar;
