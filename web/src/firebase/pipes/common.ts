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

import {GuildId} from "../../data/DiscordIds";
import {net} from "common";
import {useEffect, useState} from "react";
import {docData} from "rxfire/firestore";
import {firebaseApp} from "../setup";
import {tap} from "rxjs/operators";
import {logErrorAndRetry} from "../../rx/observer";
import GuildData = net.octyl.ourtwobe.GuildData;

export function useGuildPipe(guildId: GuildId): GuildData | undefined {
    const [guild, setGuild] = useState<GuildData>();

    useEffect(() => {
        const sub = docData<GuildData>(firebaseApp.firestore().collection("guilds").doc(guildId))
            .pipe(
                tap(setGuild),
                logErrorAndRetry(`${guildId} guild updates`)
            )
            .subscribe();

        return (): void => sub.unsubscribe();
    }, [guildId]);

    return guild;
}
