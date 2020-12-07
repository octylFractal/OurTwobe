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

import axios, {AxiosInstance, AxiosRequestConfig, Method} from "axios";

export abstract class ApiBase {
    protected readonly client: AxiosInstance;

    /**
     * A unique identifier for this API instance. Can be used to detect instance changes efficiently.
     */
    get unique(): unknown {
        return this.token;
    }

    protected constructor(
        protected readonly token: string,
        baseConfig?: AxiosRequestConfig,
    ) {
        this.client = axios.create(baseConfig);
    }

    protected async doRequest<R>(method: Method, url: string, conf: AxiosRequestConfig): Promise<R> {
        return (await this.client.request({...conf, url, method})).data;
    }
}
