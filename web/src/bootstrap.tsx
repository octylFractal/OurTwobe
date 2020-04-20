import $ from "jquery";
import "react-hot-loader";
import "./css/css";
import "./firebase/setup";
import {renderApp} from "./react-setup";

$(() => {
    const container = document.createElement("div");
    container.id = "application";
    document.body.appendChild(container);
    renderApp(container);
});
