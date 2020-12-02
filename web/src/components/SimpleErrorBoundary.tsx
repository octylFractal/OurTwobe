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
import {Alert, Modal} from "react-bootstrap";

interface SimpleErrorBoundaryProps extends React.PropsWithChildren<Record<string, unknown>> {
    context?: string;
}

interface SimpleErrorBoundaryState {
    error?: Error;
}

export class SimpleErrorBoundary extends React.Component<SimpleErrorBoundaryProps, SimpleErrorBoundaryState> {
    constructor(props: SimpleErrorBoundaryProps) {
        super(props);
        this.state = {};
    }

    static getDerivedStateFromError(error: Error): Partial<SimpleErrorBoundaryState> {
        return {
            error: error
        };
    }

    render(): ReactNode {
        if (typeof this.state.error !== "undefined") {
            const error = this.state.error;
            return <Modal show backdrop="static" size="xl">
                <Modal.Header>
                    An error occurred in {this.props.context || "the application"}.
                </Modal.Header>
                <Alert color="danger">
                    Raw Error:
                    <code className="bg-dark text-white p-1 m-1">
                        {error.message}
                    </code>
                    <details>
                        <summary>Stack</summary>
                        <pre className="bg-dark p-1">
                            {error.stack}
                        </pre>
                    </details>
                </Alert>
            </Modal>;
        }
        return this.props.children;
    }
}
