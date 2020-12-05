import React, {FormEvent, useCallback, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {useUniqueId} from "./reactHelpers";
import {useNonNullContext} from "./hook/useNonNullContext";
import {CommApiContext} from "./CommApiContext";

export const ItemSubmission: React.FC = () => {
    const commApi = useNonNullContext(CommApiContext);
    const idId = useUniqueId("video-id");
    const [value, setValue] = useState<string>("");
    const [disabled, setDisabled] = useState(false);
    const [error, setError] = useState(false);

    const onSubmit = useCallback((e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (!value) {
            return;
        }
        setError(false);
        setDisabled(true);
        commApi.submitItem({
            url: value,
        })
            .then(
                // On success: clear value
                () => setValue(""),
                // On error: do NOT clear value, set error flag
                err => {
                    setError(true);
                    console.error("Error submitting item", err);
                }
            )
            // On *: clear disabled state
            .finally(() => setDisabled(false));
    }, [value, commApi, setDisabled, setError]);

    return <Form inline onSubmit={onSubmit}>
        <Form.Group controlId={idId}>
            <Form.Label className="mx-1">Submit an item</Form.Label>
            <small className="mx-1">(video or playlist!)</small>
            <Form.Control
                className="mx-1"
                custom
                type="text"
                name="item-id"
                value={value}
                onChange={e => void setValue(e.currentTarget.value)}
                disabled={disabled}
            />
            <Button className="mx-1" type="submit" disabled={disabled || !value}>
                Submit!
            </Button>
            {error && <Form.Control.Feedback className="mx-1">Failed to submit!</Form.Control.Feedback>}
        </Form.Group>
    </Form>;
};
