import {Card, ProgressBar} from "react-bootstrap";
import React from "react";
import {PlayableItem} from "../server/api/data-pipe";

export interface PlayableItemCardProps {
    item: PlayableItem
    progress?: number
}

export const PlayableItemCard: React.FC<PlayableItemCardProps> = ({item, progress}) => {
    return <Card className="align-items-center">
        <Card.Img variant="top"
                  className="border border-black box-sizing-content-box"
                  src={item.thumbnail.url}
                  width={item.thumbnail.width}
                  height={item.thumbnail.height}
                  style={{
                      width: item.thumbnail.width,
                      height: item.thumbnail.height,
                  }}
                  alt="Thumbnail"/>
        {typeof progress !== "undefined" &&
        <Card.Body className="p-0 w-100">
            <ProgressBar
                animated variant="success" className="rounded-0"
                now={progress} min={0} max={100} label={`${progress}%`} srOnly
            />
        </Card.Body>
        }
        <Card.Body style={{maxWidth: item.thumbnail.width}}>
            <Card.Text className="text-center">{item.title}<br/>({item.youtubeId})</Card.Text>
        </Card.Body>
    </Card>;
};
