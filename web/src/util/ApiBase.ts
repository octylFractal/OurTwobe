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
