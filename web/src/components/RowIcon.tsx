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

import React from "react";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {type IconDefinition} from "@fortawesome/fontawesome-svg-core";

/**
 * The size, in pixels, of the icon at the start of a sidebar row, so the rows' text lines up.
 */
export const ROW_ICON_SIZE = 32;

export interface RowIconProps {
    icon: IconDefinition
    /**
     * A FontAwesome transform, e.g. `"right-1"`.
     */
    transform?: string
}

/**
 * A font icon centered in a {@link ROW_ICON_SIZE} box, to line up with the image icons in other rows.
 */
export const RowIcon: React.FC<RowIconProps> = ({icon, transform}) => {
    return <span className="d-inline-flex justify-content-center align-items-center flex-shrink-0 me-2"
                 style={{width: ROW_ICON_SIZE, height: ROW_ICON_SIZE}}>
        <FontAwesomeIcon icon={icon} size="lg" className="me-0" transform={transform}/>
    </span>;
};
