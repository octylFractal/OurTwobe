import React from "react";
import {DiscordApi} from "../discord/api";

export const DiscordApiContext = React.createContext<DiscordApi | null>(null);
