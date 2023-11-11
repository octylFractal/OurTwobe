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

import {type UserId} from "../../../data/DiscordIds";
import randomColor from "randomcolor";

export interface User {
    readonly id: UserId
    readonly username: string
    readonly discriminator: string
    readonly avatar: string | undefined
}

export function getAvatarUrl(user: User): string {
    if (user.avatar) {
        const ext = user.avatar.startsWith("a_") ? "gif" : "png";
        return `https://cdn.discordapp.com/avatars/${user.id}/${user.avatar}.${ext}`;
    }
    return `https://cdn.discordapp.com/embed/avatars/${parseInt(user.discriminator) % 5}.png`;
}

export function getUserNameColor(user: User): string {
    const digest = digestMessage(`${user.username}#${user.discriminator}`);
    return randomColor({
        seed: digest
    });
}

function digestMessage(message: string): number {
    let hash = 0;
    for (let i = 0; i < message.length; i++) {
        const chr = message.charCodeAt(i);
        hash = ((hash << 5) - hash) + chr;
        hash |= 0; // Convert to 32bit integer
    }
    return hash;
}
