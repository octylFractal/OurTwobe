import {net} from "common";
import React from "react";
import {NavbarImg} from "./NavbagImg";
import Server = net.octyl.ourtwobe.Server;
import classNames from "classnames";

interface ServerIconProps {
    server: Server
    width?: number
    height?: number
    className?: string
}

export const ServerIcon: React.FC<ServerIconProps> = ({server, ...props}) => {
    const width = props.width ?? 48;
    const height = props.height ?? 48;
    const classes = classNames(props.className, "rounded-circle server-icon", {
        "server-icon-text": !server.iconUrl
    });
    if (server.iconUrl) {
        return <NavbarImg className={classes} width={width} height={height} src={server.iconUrl}/>;
    }
    return <span className={classes} style={{
        width, height
    }}>
        {server.name.split(" ").map(it => it[0]).join("")}
    </span>
};
