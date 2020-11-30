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

import React, {useMemo} from "react";
import {useParams} from "react-router-dom";
import {hot} from "react-hot-loader/root";
import {newDataPipe} from "../../server/api/data-pipe";
import {DataPipeContext} from "../DataPipeContext";

interface ServerProps {
    guildId: string;
}

const Server: React.FC<ServerProps> = ({guildId}) => {
    const pipe = useMemo(() => {
        return newDataPipe(guildId);
    }, [guildId]);
    return <DataPipeContext.Provider value={pipe}>
    </DataPipeContext.Provider>;
};

const SpecificServer: React.FC = () => {
    const {guildId} = useParams<{guildId: string}>();
    if (typeof guildId === "undefined") {
        return <></>;
    }
    return <Server guildId={guildId}/>;
};

export default hot(SpecificServer);
