import React from "react";
import {useSelector} from "react-redux";
import {LocalState} from "../../redux/store";
import {net} from "common";
import {useParams} from "react-router-dom";
import Server = net.octyl.ourtwobe.Server;
import {hot} from "react-hot-loader/root";

interface ServerProps {
    serverId: string
}

const Server: React.FC<ServerProps> = ({serverId}) => {
    const serverName = useSelector((state: LocalState) =>
        (state.userInfo.profile?.servers ?? [])
            .find(value => value.id === serverId)
            ?.name ?? "Unknown server"
    );
    return <div>Server: {serverName}</div>;
};

const SpecificServer: React.FC = () => {
    const {serverId} = useParams();
    if (typeof serverId === "undefined") {
        return <></>;
    }
    return <Server serverId={serverId}/>;
};

export default hot(SpecificServer);
