import {hot} from "react-hot-loader/root";
import React from "react";
import {Jumbotron} from "reactstrap";

const Splash: React.FC = () => {
    return <Jumbotron className="text-center">
        <p className="lead">
            Welcome to <span className="font-family-audiowide">OurTwobe</span>,
            the place to share your tunes, jokes, and other miscellaneous YouTube content!
        </p>

        <p>To get started, log in to Discord in the upper-right corner.</p>
    </Jumbotron>
};

export default hot(Splash);
