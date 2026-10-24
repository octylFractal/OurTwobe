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

import React, {type FormEvent, useCallback, useState} from "react";
import {Button, Form, InputGroup} from "react-bootstrap";
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
        commApi.submitYouTubeItems({
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

    return <Form className="w-100" onSubmit={onSubmit}>
        <Form.Group controlId={idId}>
            <Form.Label className="mb-1">
                Submit an item <small>(video or playlist!)</small>
            </Form.Label>
            <InputGroup size="sm">
                <Form.Control
                    type="text"
                    name="item-id"
                    value={value}
                    onChange={e => void setValue(e.currentTarget.value)}
                    disabled={disabled}
                    isInvalid={error}
                />
                <Button type="submit" disabled={disabled || !value}>
                    Submit!
                </Button>
            </InputGroup>
            {error && <Form.Text className="d-block text-danger">Failed to submit!</Form.Text>}
            <Form.Text className="text-reset">(or even just drop files below!)</Form.Text>
        </Form.Group>
    </Form>;
};
