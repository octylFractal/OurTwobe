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

import React from "react";
import {Provider} from "react-redux";
import {BrowserRouter as Router, Route} from "react-router-dom";
import {store} from "../../redux/store";
import ScrollToTop from "../compat/ScrollToTop";
import {SimpleErrorBoundary} from "../SimpleErrorBoundary";
import {AppNavbar} from "./AppNavbar";
import {Container} from "react-bootstrap";
import Switch from "react-bootstrap/Switch";

const SplashLazy = React.lazy(async () => import("./Splash"));
const LoginHandlerLazy = React.lazy(() => import("./LoginHandler"));
const ServerLazy = React.lazy(() => import("./Server"));
const ServerNavbarLazy = React.lazy(() => import("./ServerNavbar"));

export const App: React.FC = () => {
    return <Provider store={store}>
        <SimpleErrorBoundary context="the application root">
            <Router>
                <ScrollToTop/>
                <AppNavbar/>
                <Route path="/server/:guildId/">
                    <React.Suspense fallback={<p>Loading...</p>}>
                        <ServerNavbarLazy/>
                    </React.Suspense>
                </Route>
                <Container fluid className="p-3">
                    <Switch>
                        <Route path="/discord/">
                            <React.Suspense fallback={<p>Loading...</p>}>
                                <LoginHandlerLazy/>
                            </React.Suspense>
                        </Route>
                        <Route path="/server/:guildId">
                            <React.Suspense fallback={<p>Loading...</p>}>
                                <ServerLazy/>
                            </React.Suspense>
                        </Route>
                        <Route exact path="/">
                            <React.Suspense fallback={<p>Loading...</p>}>
                                <SplashLazy/>
                            </React.Suspense>
                        </Route>
                    </Switch>
                </Container>
            </Router>
        </SimpleErrorBoundary>
    </Provider>;
};
