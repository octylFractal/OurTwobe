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
