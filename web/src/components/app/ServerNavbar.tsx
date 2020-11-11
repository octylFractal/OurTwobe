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

import React, {useState} from "react";
import {useParams} from "react-router-dom";
import {hot} from "react-hot-loader/root";
import {Collapse, Form, Nav, Navbar, NavbarBrand, NavbarToggler} from "reactstrap";
import {ServerIcon} from "../ServerIcon";
import {ChannelSelect} from "../ChannelSelect";
import {useGuildPipe} from "../../firebase/pipes/common";

interface ServerNavbarProps {
    guildId: string;
}

const ServerNavbar: React.FC<ServerNavbarProps> = ({guildId}) => {
    const [isOpen, setOpen] = useState(false);
    const toggle = () => void setOpen(prevState => !prevState);

    const guild = useGuildPipe(guildId);
    if (!guild) {
        return <></>;
    }

    return <Navbar color="dark" dark expand="md">
        <NavbarBrand className="py-1">
            <span className="ourtwobe-at-server">
                <h4 className="font-family-audiowide d-inline align-middle">{' @ '}</h4>
                <ServerIcon guildData={guild} className="mr-3" width={32} height={32}/>
                <span className="font-family-audiowide text-wrap">{guild.name}</span>
            </span>
        </NavbarBrand>
        <NavbarToggler onClick={toggle} className="mx-auto"/>
        <Collapse isOpen={isOpen} navbar>
            <Nav className="mr-auto" navbar>
                <Form inline>
                    <ChannelSelect guildId={guildId}/>
                </Form>
            </Nav>
        </Collapse>
    </Navbar>;
};

const SpecificServerNavbar: React.FC = () => {
    const {guildId} = useParams<{guildId: string}>();
    if (typeof guildId === "undefined") {
        return <></>;
    }
    return <ServerNavbar guildId={guildId}/>;
};

export default hot(SpecificServerNavbar);
