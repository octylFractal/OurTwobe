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

import {User} from "./response/User";
import axios, {AxiosInstance} from "axios";

export class DiscordApi {
    private readonly client: AxiosInstance;

    constructor(token: string) {
        this.client = axios.create({
            baseURL: "https://discord.com/api/v8/",
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });
    }

    async getMe(): Promise<User> {
        return this.client.get("/me");
    }
}
