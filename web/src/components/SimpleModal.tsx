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

import React, {ReactNode} from "react";
import {SimpleErrorBoundary} from "./SimpleErrorBoundary";
import {Button, Modal, ModalProps} from "react-bootstrap";

export type SimpleModalProps = React.PropsWithChildren<{
    title: string;
    submitLabel: string;
    isOpen: boolean;
    closeModal: () => void;
    onSubmit: React.MouseEventHandler<HTMLElement>;
    size?: ModalProps['size'];
    footer?: ReactNode;
}>;

export const SimpleModal: React.FC<SimpleModalProps> = (
    {title, submitLabel, isOpen, closeModal, onSubmit, size, children, footer}
) => {
    return <SimpleErrorBoundary context={`a modal dialog: '${title}'`}>
        <Modal show={isOpen} onHide={closeModal} backdrop='static' size={size}>
            <Modal.Header onHide={closeModal}>
                {title}
            </Modal.Header>
            <Modal.Body className="p-3">
                {children}
            </Modal.Body>
            <Modal.Footer>
                {footer}
                <Button color="secondary" onClick={(e): void => {
                    e.preventDefault();
                    closeModal();
                }}>Cancel</Button>
                <Button color="primary" onClick={(e): void => {
                    e.preventDefault();
                    closeModal();
                    onSubmit(e);
                }}>{submitLabel}</Button>
            </Modal.Footer>
        </Modal>
    </SimpleErrorBoundary>;
};
