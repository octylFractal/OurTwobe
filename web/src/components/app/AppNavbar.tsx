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
import {Collapse, Nav, Navbar, NavbarBrand, NavbarToggler, NavItem} from "reactstrap";
import RoutedNavLink from "../compat/RoutedNavLink";
import {LocalUserState} from "../LocalUserState";
import Logo from "../../app/logo.png";
import {NavbarImg} from "../NavbagImg";
import {LocalServerDropdown} from "../LocalServerDropdown";

export const AppNavbar: React.FC = () => {
    const [isOpen, setOpen] = useState(false);
    const toggle = () => void setOpen(prevState => !prevState);

    return <Navbar color="dark" dark expand="md">
        <NavbarBrand href="/" className="py-3 pl-0">
            <NavbarImg src={Logo} alt="Logo"/>
            <h2 className="font-family-audiowide d-inline align-middle flex-grow-1">OurTwobe</h2>
        </NavbarBrand>
        <NavbarToggler onClick={toggle} className="mx-auto"/>
        <Collapse isOpen={isOpen} navbar className="align-self-stretch align-items-stretch">
            <Nav className="mr-auto" navbar>
                <RoutedNavLink exact to="/">Home</RoutedNavLink>
                <LocalServerDropdown/>
            </Nav>
            <Nav className="ml-auto" navbar>
                <NavItem>
                    <LocalUserState/>
                </NavItem>
            </Nav>
        </Collapse>
    </Navbar>;
};
