import React from "react";
import ReactDOM from "react-dom";
import {App} from "./components/app/App";

export function renderApp(container: Element) {
    ReactDOM.render(<App/>, container);
}
