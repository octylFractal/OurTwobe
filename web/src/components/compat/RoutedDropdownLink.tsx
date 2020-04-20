import React, {ReactNode} from "react";
import {NavLink} from "react-router-dom";
import {DropdownItem} from "reactstrap";

export interface RoutedDropdownLinkProps {
    to: string
    exact?: boolean
    className?: string
    children?: ReactNode
}

export const RoutedDropdownLink: React.FC<RoutedDropdownLinkProps> = (props) => {
    return <DropdownItem tag={NavLink}
                         activeClassName="active"
                         {...props}/>;
};
