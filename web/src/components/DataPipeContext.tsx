import React from "react";
import {DataPipe} from "../server/api/data-pipe";

export const DataPipeContext = React.createContext<DataPipe | null>(null);
