import {Dropdown, DropdownItem, DropdownMenu, DropdownToggle} from "reactstrap";
import React, {useState} from "react";
import {RoutedDropdownLink} from "./compat/RoutedDropdownLink";
import classNames from "classnames";
import {net} from "common";
import {NavbarImg} from "./NavbagImg";
import Server = net.octyl.ourtwobe.Server;
import {ServerIcon} from "./ServerIcon";

export interface SeriesDropdownProps {
    loggedIn: boolean
    servers: Server[]
}

export const ServerDropdown: React.FC<SeriesDropdownProps> = ({loggedIn, servers}) => {
    if (!loggedIn) {
        return <></>;
    }

    let [open, setOpen] = useState(false);
    const toggle = () => setOpen(prevState => !prevState);

    const classes = classNames({
        disabled: servers.length === 0
    });

    return <Dropdown isOpen={open} toggle={toggle} nav inNavbar>
        <DropdownToggle nav caret className={classes}>
            Server
        </DropdownToggle>
        <DropdownMenu>
            {servers.length === 0
                ? <DropdownItem disabled>No servers.</DropdownItem>
                : servers.map(server =>
                    <RoutedDropdownLink
                        to={`/server/${server.id}`}
                        key={server.id}>
                        <ServerIcon server={server}/>
                        {server.name}
                    </RoutedDropdownLink>
                )}
        </DropdownMenu>
    </Dropdown>
};
