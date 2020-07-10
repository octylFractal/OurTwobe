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

import React, {ReactNode} from "react";
import {NavLink, NavLinkProps} from "react-router-dom";

export interface RoutedNavLinkProps extends NavLinkProps {
    children?: ReactNode;
}

const RoutedNavLink: React.FC<RoutedNavLinkProps> = ({children, ...props}) => {
    return <li className="nav-item">
        <NavLink className="routed nav-link" activeClassName="active" {...props}>
            {children}
        </NavLink>
    </li>;
};

export default RoutedNavLink;
