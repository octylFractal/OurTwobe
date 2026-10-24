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
import {useLocation} from "react-router";
import {Button, CloseButton, ListGroup, Offcanvas} from "react-bootstrap";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faBars} from "@fortawesome/free-solid-svg-icons/faBars";
import {faHouse} from "@fortawesome/free-regular-svg-icons/faHouse";
import {faCircleUser} from "@fortawesome/free-solid-svg-icons/faCircleUser";
import {faSpinner} from "@fortawesome/free-solid-svg-icons/faSpinner";
import classNames from "classnames";
import RoutedNavLink from "../compat/RoutedNavLink";
import {LocalUserState} from "../LocalUserState";
import {LocalServerList} from "../LocalServerList";
import {DiscordApiProvider} from "../DiscordApiContext";
import {SimpleErrorBoundary} from "../SimpleErrorBoundary";
import {useUniqueId} from "../reactHelpers";
import {SUBTLE_BORDER} from "../../css/classes";
import {RowIcon} from "../RowIcon";

const LOGO_URL = new URL("../../app/logo.png?w=32&h=32", import.meta.url).toString();
const LOGO_ALT = "YouTube logo with colors inverted and a red hammer and sickle";

const SIDEBAR_CLASSES = "bg-dark sticky-md-top vh-100 overflow-y-auto flex-shrink-0";
const SIDEBAR_STYLE = {width: "16rem"};

/**
 * The page layout: navigation on the left, the user menu on the right, and the page in between. Below the md
 * breakpoint, the sidebars are hidden behind buttons in a top bar.
 */
export const AppLayout: React.FC<React.PropsWithChildren> = ({children}) => {
    const navId = useUniqueId("nav-sidebar");
    const userId = useUniqueId("user-sidebar");
    const [open, setOpen] = useState<"nav" | "user" | null>(null);
    const location = useLocation();
    // close the mobile sidebars after navigating
    const [lastLocation, setLastLocation] = useState(location);
    if (location !== lastLocation) {
        setLastLocation(location);
        setOpen(null);
    }
    const close = (): void => void setOpen(null);

    return <>
        <div className="d-flex d-md-none align-items-center gap-2 p-2 bg-dark border-bottom">
            <Button variant="dark" size="sm" onClick={() => void setOpen("nav")} aria-controls={navId} aria-label="Open navigation">
                <FontAwesomeIcon icon={faBars} className="me-0"/>
            </Button>
            <img src={LOGO_URL} width={32} height={32} alt={LOGO_ALT}/>
            <span className="h4 mb-0 font-family-audiowide">OurTwobe</span>
            <Button variant="dark" size="sm" className="ms-auto" onClick={() => void setOpen("user")} aria-controls={userId} aria-label="Open user menu">
                <FontAwesomeIcon icon={faCircleUser} className="me-0"/>
            </Button>
        </div>
        <div className="d-md-flex">
            <Offcanvas
                id={navId}
                responsive="md"
                show={open === "nav"}
                onHide={close}
                className={classNames("app-sidebar", SIDEBAR_CLASSES)}
                style={SIDEBAR_STYLE}
            >
                <Offcanvas.Header className="d-md-flex">
                    <Offcanvas.Title className="d-flex align-items-center gap-2 font-family-audiowide">
                        <img src={LOGO_URL} width={32} height={32} alt={LOGO_ALT}/>
                        OurTwobe
                    </Offcanvas.Title>
                    <CloseButton variant="white" className="d-md-none" onClick={close}/>
                </Offcanvas.Header>
                <Offcanvas.Body className="flex-grow-1 p-0">
                    <nav className="d-flex flex-column gap-3 w-100">
                        <ListGroup variant="flush">
                            <RoutedNavLink to="/" end className="d-flex align-items-center">
                                <RowIcon icon={faHouse}/>
                                Home
                            </RoutedNavLink>
                        </ListGroup>
                        <DiscordApiProvider fallback={<></>}>
                            <ListGroup variant="flush">
                                <SimpleErrorBoundary context="the server list">
                                    <React.Suspense fallback={
                                        <ListGroup.Item><FontAwesomeIcon spin icon={faSpinner}/></ListGroup.Item>
                                    }>
                                        <LocalServerList/>
                                    </React.Suspense>
                                </SimpleErrorBoundary>
                            </ListGroup>
                        </DiscordApiProvider>
                    </nav>
                </Offcanvas.Body>
            </Offcanvas>
            <main className="flex-grow-1" style={{minWidth: 0}}>
                {children}
            </main>
            <Offcanvas
                id={userId}
                responsive="md"
                placement="end"
                aria-label="User menu"
                show={open === "user"}
                onHide={close}
                className={classNames("border-start", SUBTLE_BORDER, SIDEBAR_CLASSES)}
                style={SIDEBAR_STYLE}
            >
                <Offcanvas.Header closeButton closeVariant="white">
                    <Offcanvas.Title>User</Offcanvas.Title>
                </Offcanvas.Header>
                <Offcanvas.Body className="flex-grow-1 p-0">
                    <ListGroup variant="flush" className="w-100">
                        <LocalUserState/>
                    </ListGroup>
                </Offcanvas.Body>
            </Offcanvas>
        </div>
    </>;
};
