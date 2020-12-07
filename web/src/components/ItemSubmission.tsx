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
