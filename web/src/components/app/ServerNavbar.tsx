import React from "react";
import {useSelector} from "react-redux";
import {LocalState} from "../../redux/store";
import {useParams} from "react-router-dom";
import {hot} from "react-hot-loader/root";
import {Navbar, NavbarBrand} from "reactstrap";
import {ServerIcon} from "../ServerIcon";

interface ServerNavbarProps {
    serverId: string
}

const ServerNavbar: React.FC<ServerNavbarProps> = ({serverId}) => {
    const server = useSelector((state: LocalState) =>
        (state.userInfo.profile?.servers ?? [])
            .find(value => value.id === serverId)
    );
    if (!server) {
        return <></>;
    }
    return <Navbar color="dark" dark expand="md">
        <NavbarBrand className="py-1">
            <span className="ourtwobe-at-server">
                <h4 className="font-family-audiowide d-inline align-middle">{' @ '}</h4>
                <ServerIcon server={server} className="mr-3" width={32} height={32}/>
                <span className="font-family-audiowide text-wrap">{server.name}</span>
            </span>
        </NavbarBrand>
    </Navbar>;
};

const SpecificServerNavbar: React.FC = () => {
    const {serverId} = useParams();
    if (typeof serverId === "undefined") {
        return <></>;
    }
    return <ServerNavbar serverId={serverId}/>;
};

export default hot(SpecificServerNavbar);
