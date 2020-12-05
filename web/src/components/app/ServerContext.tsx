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

interface RealServerContextProps {
    guildId: GuildId
    token: string
}

const RealServerContext: React.FC<RealServerContextProps> = ({guildId, token, children}) => {
    const commApi = useMemo(() => new OurTwobeCommApi(token, guildId), [token, guildId]);
    const dispatch = useDispatch();
    useEffect(() => {
        const subscription = newDataPipe(guildId).observable.pipe(
            tap(subscribeToEventsFunc(guildId, commApi, dispatch))
        ).subscribe();

        return () => void subscription.unsubscribe();
    }, [guildId, commApi, dispatch]);
    return <DiscordApiProvider>
        <CommApiContext.Provider value={commApi}>
            {children}
        </CommApiContext.Provider>
    </DiscordApiProvider>;
};

function subscribeToEventsFunc(guildId: string, api: OurTwobeCommApi, dispatch: Dispatch): (e: DataPipeEvent | DataPipeError) => void {
    return (e): void => {
        if ("error" in e) {
            if (e.value instanceof Event && "type" in e.value && e.value.type === "error") {
                // This is a disconnect error, potentially our session expired
                api.authenticate()
                    .catch(err => console.error("Failed to re-authenticate, server down?", err));
                return;
            }
            console.warn("Error in data pipe:", e.value);
            return;
        }
        switch (e.type) {
            case "guildSettings":
                dispatch(guildState.updateState({...e, guildId}));
                break;
            case "progressItem":
                break;
            case "queueItem":
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
    if (typeof guildId === "undefined") {
        return <></>;
    }
    if (!token) {
        // Dump them back in the log in page
        return <Redirect to="/"/>;
    }
    return <RealServerContext guildId={guildId} token={token}>{children}</RealServerContext>;
};

export default ServerContext;
