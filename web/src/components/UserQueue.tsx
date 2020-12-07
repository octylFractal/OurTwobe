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
