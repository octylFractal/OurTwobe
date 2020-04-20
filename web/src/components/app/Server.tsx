import React from "react";
import {useSelector} from "react-redux";
import {LocalState} from "../../redux/store";
import {net} from "common";
import {useParams} from "react-router-dom";
import {hot} from "react-hot-loader/root";
import {Navbar, NavbarBrand} from "reactstrap";
import {ServerIcon} from "../ServerIcon";
import Server = net.octyl.ourtwobe.Server;

interface ServerProps {
    serverId: string
}

const Server: React.FC<ServerProps> = ({serverId}) => {
    const server = useSelector((state: LocalState) =>
        (state.userInfo.profile?.servers ?? [])
            .find(value => value.id === serverId)
    );
    if (!server) {
        return <></>;
    }
    return <div>
    </div>;
};

const SpecificServer: React.FC = () => {
    const {serverId} = useParams();
    if (typeof serverId === "undefined") {
        return <></>;
    }
    return <Server serverId={serverId}/>;
};

export default hot(SpecificServer);
