import {Button, Card, ProgressBar} from "react-bootstrap";
import React from "react";
import {NO_PLAYING_ITEM, PlayableItem} from "../server/api/data-pipe";
import {useNonNullContext} from "./hook/useNonNullContext";
import {CommApiContext} from "./CommApiContext";

export interface PlayableItemCardProps {
    item: PlayableItem
    progress?: number
}

export const PlayableItemCard: React.FC<PlayableItemCardProps> = ({item, progress}) => {
    const commApi = useNonNullContext(CommApiContext);

    const controlsDisabled = item.id === NO_PLAYING_ITEM.item.id;

    const controlRemove = typeof progress === "undefined"
        ? {label: "Remove", action: () => void commApi.removeItem({itemId: item.id})}
        : {label: "Skip", action: () => void commApi.skipItem({itemId: item.id})};

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
        <Card.Body className="p-0 w-100">
            <Button
                className="p-0 w-100 rounded-0"
                variant={controlsDisabled ? "dark" : "danger"}
                disabled={controlsDisabled}
                onClick={controlRemove.action}>
                {controlRemove.label}
            </Button>
        </Card.Body>
    </Card>;
};
