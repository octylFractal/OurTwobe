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

import React, {type ReactNode} from "react";
import {NavLink, type NavLinkProps} from "react-router";
import classNames from "classnames";

export interface RoutedNavLinkProps extends Omit<NavLinkProps, "className"> {
    className?: string;
    children?: ReactNode;
}

/**
 * A {@link NavLink} styled as a list group item, for use in a list group.
 */
const RoutedNavLink: React.FC<RoutedNavLinkProps> = ({children, className, ...props}) => {
    return <NavLink
        className={({isActive}): string => classNames(
            "list-group-item list-group-item-action", className, {active: isActive}
        )}
        {...props}
    >
        {children}
    </NavLink>;
};

export default RoutedNavLink;
