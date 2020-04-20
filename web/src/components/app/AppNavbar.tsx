import React, {useState} from "react";
import {Collapse, Nav, Navbar, NavbarBrand, NavbarToggler, NavItem} from "reactstrap";
import RoutedNavLink from "../compat/RoutedNavLink";
import {LocalUserState} from "../LocalUserState";
import Logo from "../../app/logo.png";
import {NavbarImg} from "../NavbagImg";
import {LocalServerDropdown} from "../LocalServerDropdown";

export const AppNavbar: React.FC = () => {
    const [isOpen, setOpen] = useState(false);

    function toggle() {
        setOpen(!isOpen);
    }

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
