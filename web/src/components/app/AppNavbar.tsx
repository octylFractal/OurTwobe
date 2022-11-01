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

import React from "react";
import RoutedNavLink from "../compat/RoutedNavLink";
import {LocalUserState} from "../LocalUserState";
import Logo from "../../app/logo.png";
import {NavbarImg} from "../NavbagImg";
import {LocalServerDropdown} from "../LocalServerDropdown";
import {Container, Nav, Navbar} from "react-bootstrap";
import {useUniqueId} from "../reactHelpers";
import {DiscordApiProvider} from "../DiscordApiContext";
import {ServerDropdown} from "../ServerDropdown";

export const AppNavbar: React.FC = () => {
    const id = useUniqueId("collapse");

    return <Navbar variant="dark" bg="dark" expand="md">
        <Container fluid>
            <Navbar.Brand href="/" className="py-3 pl-0">
                <NavbarImg src={Logo} alt="Logo"/>
                <h2 className="font-family-audiowide d-inline align-middle flex-grow-1">OurTwobe</h2>
            </Navbar.Brand>
            <Navbar.Toggle aria-controls={id} className="mx-auto"/>
            <Navbar.Collapse id={id}>
                <Nav className="me-auto">
                    <RoutedNavLink to="/" end>Home</RoutedNavLink>
                    <DiscordApiProvider fallback={<></>}>
                        <React.Suspense fallback={<ServerDropdown guilds={[]}/>}>
                            <LocalServerDropdown/>
                        </React.Suspense>
                    </DiscordApiProvider>
                </Nav>
                <Nav className="ms-auto">
                    <Nav.Item>
                        <LocalUserState/>
                    </Nav.Item>
                </Nav>
            </Navbar.Collapse>
        </Container>
    </Navbar>;
};
