import React, {ReactNode} from "react";
import {NavLink, NavLinkProps} from "react-router-dom";

export interface RoutedNavLinkProps extends NavLinkProps {
    children?: ReactNode
}

const RoutedNavLink: React.FC<RoutedNavLinkProps> = ({children, ...props}) => {
    return <li className="nav-item">
        <NavLink className="routed nav-link" activeClassName="active" {...props}>
            {children}
        </NavLink>
    </li>;
};

export default RoutedNavLink;
