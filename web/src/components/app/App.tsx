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

const LoginHandlerLazy = loadable(() => import("./LoginHandler"));
const ServerLazy = loadable(() => import("./Server"));

const HotPortion = hot(() => {
    return <SimpleErrorBoundary context="the application root">
        <AppNavbar/>
        <Container fluid={true} className="p-3">
            <Switch>
                <Route path="/discord/">
                    <LoginHandlerLazy/>
                </Route>
                <Route path="/server/:serverId">
                    <ServerLazy/>
                </Route>
                <Route path="/">
                    <p>TODO!</p>
                    {/*<UserTimeCardsLazy/>*/}
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
