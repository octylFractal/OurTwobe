import {net} from "common";
import React from "react";
import {NavbarImg} from "./NavbagImg";
import Server = net.octyl.ourtwobe.Server;

interface ServerIconProps {
    server: Server
}

export const ServerIcon: React.FC<ServerIconProps> = ({server}) => {
    if (server.iconUrl) {
        return <NavbarImg className="rounded-circle server-icon" src={server.iconUrl}/>;
    }
    return <span className="rounded-circle server-icon server-icon-text">
        {server.name.split(" ").map(it => it[0]).join("")}
    </span>
};
