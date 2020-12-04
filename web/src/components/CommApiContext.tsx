import React from "react";
import {OurTwobeCommApi} from "../server/api/communication";

export const CommApiContext = React.createContext<OurTwobeCommApi | null>(null);
