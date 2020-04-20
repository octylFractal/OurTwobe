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

import loadable from "@loadable/component";
import React from "react";
import {hot} from "react-hot-loader/root";
import {Provider} from "react-redux";
import {BrowserRouter as Router, Route, Switch} from "react-router-dom";
import {Container} from "reactstrap";
import {store} from "../../redux/store";
import ScrollToTop from "../compat/ScrollToTop";
import {SimpleErrorBoundary} from "../SimpleErrorBoundary";
import {AppNavbar} from "./AppNavbar";

const SplashLazy = loadable(() => import("./Splash"));
const LoginHandlerLazy = loadable(() => import("./LoginHandler"));
const ServerLazy = loadable(() => import("./Server"));
const ServerNavbarLazy = loadable(() => import("./ServerNavbar"));

const HotPortion = hot(() => {
    return <SimpleErrorBoundary context="the application root">
        <AppNavbar/>
        <Switch>
            <Route path="/server/:serverId/">
                <ServerNavbarLazy/>
            </Route>
        </Switch>
        <Container fluid={true} className="p-3">
            <Switch>
                <Route path="/discord/">
                    <LoginHandlerLazy/>
                </Route>
                <Route path="/server/:serverId">
                    <ServerLazy/>
                </Route>
                <Route path="/">
                    <SplashLazy/>
                </Route>
            </Switch>
        </Container>
    </SimpleErrorBoundary>;
});

export const App = () => {
    return <Router>
        <ScrollToTop/>
        <Provider store={store}>
            <HotPortion/>
        </Provider>
    </Router>;
};
