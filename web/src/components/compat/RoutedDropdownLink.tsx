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
import {NavLink} from "react-router-dom";
import {Dropdown} from "react-bootstrap";

export interface RoutedDropdownLinkProps {
    to: string;
    exact?: boolean;
    className?: string;
    children?: ReactNode;
}

export const RoutedDropdownLink: React.FC<RoutedDropdownLinkProps> = (props) => {
    return <Dropdown.Item as={NavLink} active {...props}/>;
};
