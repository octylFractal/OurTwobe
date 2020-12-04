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
import {BrowserRouter as Router, Route, Switch} from "react-router-dom";
import ScrollToTop from "../compat/ScrollToTop";
import {SimpleErrorBoundary} from "../SimpleErrorBoundary";
import {AppNavbar} from "./AppNavbar";
import {Container} from "react-bootstrap";
import {ExposeStore} from "../../redux/store";

const SplashLazy = React.lazy(async () => import("./Splash"));
const LoginHandlerLazy = React.lazy(() => import("./LoginHandler"));
const ServerLazy = React.lazy(() => import("./Server"));
const ServerContextLazy = React.lazy(() => import("./ServerContext"));
const ServerNavbarLazy = React.lazy(() => import("./ServerNavbar"));

const CommonContainer: React.FC = ({children}) => <Container fluid className="p-3">{children}</Container>;

export const App: React.FC = () => {
    return <ExposeStore>
        <SimpleErrorBoundary context="the application root">
            <Router>
                <ScrollToTop/>
                <AppNavbar/>
                <Switch>
                    <Route path="/discord/">
                        <React.Suspense fallback={<p>Loading...</p>}>
                            <CommonContainer>
                                <LoginHandlerLazy/>
                            </CommonContainer>
                        </React.Suspense>
                    </Route>
                    <Route path="/server/:guildId">
                        <React.Suspense fallback={<p>Loading...</p>}>
                            <ServerContextLazy>
                                <ServerNavbarLazy/>
                                <CommonContainer>
                                    <ServerLazy/>
                                </CommonContainer>
                            </ServerContextLazy>
                        </React.Suspense>
                    </Route>
                    <Route exact path="/">
                        <React.Suspense fallback={<p>Loading...</p>}>
                            <CommonContainer>
                                <SplashLazy/>
                            </CommonContainer>
                        </React.Suspense>
                    </Route>
                </Switch>
            </Router>
        </SimpleErrorBoundary>
    </ExposeStore>;
};
