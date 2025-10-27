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

import React, {useCallback, useState} from "react";
import {useNonNullContext} from "./hook/useNonNullContext";
import {CommApiContext} from "./CommApiContext";

function getTransferFiles(e: React.DragEvent<HTMLDivElement>): DataTransferItem[] {
    return Array.from(e.dataTransfer.items).filter(i => i.kind === "file");
}

export const DragDropSubmission: React.FC<React.PropsWithChildren> = ({children}) => {
    const commApi = useNonNullContext(CommApiContext);
    const [disabled, setDisabled] = useState(false);

    const onDrop = useCallback((e: React.DragEvent<HTMLDivElement>) => {
        const dtiFiles = getTransferFiles(e);
        if (!dtiFiles) {
            // Don't handle non-file drops
            return;
        }
        e.preventDefault();
        if (disabled) {
            // Ignore drops while disabled
            return;
        }
        const form = new FormData();
        for (const dti of dtiFiles) {
            const file = dti.getAsFile();
            if (file) {
                form.append("files", file, file.name);
            }
        }
        setDisabled(true);
        commApi.submitFileItems(form)
            .then(
                () => {
                },
                err => {
                    console.error("Error submitting items", err);
                }
            )
            // On *: clear disabled state
            .finally(() => setDisabled(false));
    }, [commApi, disabled, setDisabled]);

    const onDragOver = useCallback((e: React.DragEvent<HTMLDivElement>) => {
        const dtiFiles = getTransferFiles(e);
        if (!dtiFiles) {
            // Don't handle non-file drops
            return;
        }
        e.preventDefault();
        e.dataTransfer.dropEffect = "copy";
    }, []);

    return <div className="drag-and-drop-target" onDrop={onDrop} onDragOver={onDragOver}>
        {children}
    </div>;
};
