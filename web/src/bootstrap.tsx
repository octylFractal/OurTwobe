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

import $ from "jquery";
import "react-hot-loader";
import "./css/css";
import "./firebase/setup";
import {renderApp} from "./react-setup";
import {subscribeSelectors} from "./rx/subscribeSelectors";

$(() => {
    const container = document.createElement("div");
    container.id = "application";
    document.body.appendChild(container);
    subscribeSelectors();
    renderApp(container);
});
