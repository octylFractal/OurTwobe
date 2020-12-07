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
import {getAvatarUrl, getUserNameColor, User} from "../discord/api/response/User";
import {PlayableItem} from "../server/api/data-pipe";
import {Card, ListGroup} from "react-bootstrap";
import {PlayableItemCard} from "./PlayableItemCard";
import {useNonNullContext} from "./hook/useNonNullContext";
import {DiscordApiContext} from "./DiscordApiContext";
import {useAutoFetch} from "./fetchstore/patch";
import {UserId} from "../data/DiscordIds";
import {NavbarImg} from "./NavbagImg";

export interface UserQueueProps {
    owner: User
    items: PlayableItem[]
}

export const UserQueue: React.FC<UserQueueProps> = ({owner, items}) => {
    return <Card>
        <Card.Header className="text-center">
            <img alt="Avatar"
                 className="rounded-circle border border-light mr-3"
                 src={getAvatarUrl(owner)}
                 width={40}
                 height={40}
            />
            <span style={{color: getUserNameColor(owner)}}>{owner.username}&apos;s</span> Queue
        </Card.Header>
        <Card.Body>
            <div className="d-flex flex-column">
                {items.map(item => <div key={item.id} className="my-2">
                    <PlayableItemCard item={item}/>
                </div>)}
            </div>
        </Card.Body>
    </Card>;
};

export interface UserIdQueueProps {
    ownerId: UserId
    items: PlayableItem[]
}

export const UserIdQueue: React.FC<UserIdQueueProps> = ({ownerId, items}) => {
    const discordApi = useNonNullContext(DiscordApiContext);
    const owner = useAutoFetch(discordApi.fetch.users, ownerId);
    return <UserQueue owner={owner} items={items}/>;
};
