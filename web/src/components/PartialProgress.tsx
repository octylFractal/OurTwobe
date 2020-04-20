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
import {Progress} from "reactstrap";

export interface PartialProgressProps {
    percentage: number;
    start: number;
    cap: number;
    color: string;
}

export const PartialProgress: React.FC<PartialProgressProps> = ({percentage, start, cap, color}) => {
    if (percentage >= start) {
        const realPercent = Math.min(cap, percentage);
        return <Progress animated bar color={color} value={realPercent - start}/>;
    }
    return null;
};