import React, {useEffect, useMemo} from "react";
import {DiscordApiProvider} from "../DiscordApiContext";
import {useParams} from "react-router-dom";
import {useDispatch, useSelector} from "react-redux";
import {LocalState} from "../../redux/store";
import {Redirect} from "react-router";
import {DataPipeError, DataPipeEvent, newDataPipe} from "../../server/api/data-pipe";
import {CommApiContext} from "../CommApiContext";
import {OurTwobeCommApi} from "../../server/api/communication";
import {GuildId} from "../../data/DiscordIds";
import {tap} from "rxjs/operators";
import {exhaustiveCheck} from "../../utils";
import {Dispatch} from "redux";
import {guildState} from "../../redux/reducer";
import {AxiosError} from "axios";

interface RealServerContextProps {
    guildId: GuildId
    token: string
}

const RealServerContext: React.FC<RealServerContextProps> = ({guildId, token, children}) => {
    const commApi = useMemo(() => new OurTwobeCommApi(token, guildId), [token, guildId]);
    const dispatch = useDispatch();
    useEffect(() => {
        const reAuth = async (): Promise<void> => {
            try {
                await commApi.authenticate();
            } catch (e) {
                const axios = e as AxiosError;
                if ("response" in axios && axios.response?.status === 401) {
                    // new discord token needed? refresh the page to figure it out
                    window.location.reload();
                }
                throw e;
            }
        };
        const subscription = newDataPipe(guildId, reAuth).observable.pipe(
            tap(subscribeToEventsFunc(guildId, reAuth, dispatch))
        ).subscribe();

        return () => void subscription.unsubscribe();
    }, [guildId, commApi, dispatch]);
    return <DiscordApiProvider>
        <CommApiContext.Provider value={commApi}>
            {children}
        </CommApiContext.Provider>
    </DiscordApiProvider>;
};

function subscribeToEventsFunc(guildId: string, authenticate: () => Promise<void>, dispatch: Dispatch): (e: DataPipeEvent | DataPipeError) => void {
    return (e): void => {
        if ("error" in e) {
            if (e.value instanceof Event && "type" in e.value && e.value.type === "error") {
                // This is a disconnect error, potentially our session expired
                return;
            }
            console.warn("Error in data pipe:", e.value);
            return;
        }
        switch (e.type) {
            case "guildSettings":
                dispatch(guildState.updateSettings({...e, guildId}));
                break;
            case "progressItem":
                dispatch(guildState.updatePlayingItem({...e, guildId}));
                break;
            case "removeItem":
                dispatch(guildState.removeQueuedItem({...e, guildId}));
                break;
            case "queueItem":
                dispatch(guildState.addQueuedItem({...e, guildId}));
                break;
            case "clearQueues":
                dispatch(guildState.clearQueues({guildId}));
                break;
            default:
                exhaustiveCheck(e);
        }
    };
}

/**
 * Set up each context element needed for working in a server.
 *
 * @param children the elements to render in the context
 */
const ServerContext: React.FC = ({children}) => {
    const {guildId} = useParams<{ guildId: string }>();
    const token = useSelector((state: LocalState) => state.userToken);
    if (!token) {
        // Dump them back in the log in page
        return <Redirect to="/"/>;
    }
    return <RealServerContext guildId={guildId} token={token}>{children}</RealServerContext>;
};

export default ServerContext;
