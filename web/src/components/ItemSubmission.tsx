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
import {Button, Col, Form, Row} from "react-bootstrap";
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

    return <Form className="flex-fill ps-3" onSubmit={onSubmit}>
        <Form.Group as={Row} className="align-items-center" controlId={idId}>
            <Form.Label column className="text-center">
                Submit an item<br/>
                <small>(video or playlist!)</small>
            </Form.Label>
            <Col>
                <Form.Control
                    className="mx-1"
                    type="text"
                    size="sm"
                    name="item-id"
                    value={value}
                    onChange={e => void setValue(e.currentTarget.value)}
                    disabled={disabled}
                />
            </Col>
            <Col>
                <Button className="mx-1" type="submit" size="sm" disabled={disabled || !value}>
                    Submit!
                </Button>
            </Col>
            {error && <Col><Form.Control.Feedback className="mx-1">Failed to submit!</Form.Control.Feedback></Col>}
        </Form.Group>
    </Form>
;
};
