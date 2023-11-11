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

import React, {useMemo} from "react";
import {useSelector} from "react-redux";
import {type LocalState} from "../../redux/store";
import {useParams} from "react-router-dom";
import {UserIdQueue} from "../UserQueue";
import {PlayableItemCard} from "../PlayableItemCard";
import {NO_PLAYING_ITEM} from "../../server/api/data-pipe";
import {Comparators} from "../../util/compare";
import {requireNonNull} from "../../utils";
import {createSimpleSelector} from "../../redux/selectors";

const Server: React.FC = () => {
    const {guildId} = useParams<{ guildId: string }>();
    requireNonNull(guildId);
    const queueSelector = useMemo(
        () => createSimpleSelector((state: LocalState) => state.guildState[guildId]?.queues || {}),
        [guildId]
    );
    const queues = useSelector(queueSelector);
    const nowPlayingSelector = useMemo(
        () => createSimpleSelector((state: LocalState) => state.guildState[guildId]?.playing || NO_PLAYING_ITEM),
        [guildId]
    );
    const nowPlaying = useSelector(nowPlayingSelector);
    return <div className="d-flex flex-column">
        <div className="d-inline-flex flex-column align-items-center">
            <p>Now playing:</p>
            <PlayableItemCard item={nowPlaying.item} progress={nowPlaying.progress}/>
        </div>
        <div className="my-3 border-bottom border-light"/>
        <div className="d-flex flex-row flex-grow-1">
            {Object.entries(queues)
                .filter(([, queue]) => queue.items.length > 0)
                .sort(Comparators.comparing(([, q]) => q.items[0].submissionTime, Comparators.NUMBER))
                .map(([key, queue]) => {
                return <UserIdQueue key={key} ownerId={key} items={queue.items}/>;
            })}
        </div>
    </div>;
};

export default Server;
