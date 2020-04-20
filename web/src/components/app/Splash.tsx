import {hot} from "react-hot-loader/root";
import React from "react";
import {Jumbotron} from "reactstrap";

const Splash: React.FC = () => {
    return <div>
        <Jumbotron className="text-center">
            <p className="lead">
                Welcome to <span className="font-family-audiowide">OurTwobe</span>,
                the place to share your tunes, jokes, and other miscellaneous YouTube content!
            </p>

            <p>To get started, log in to Discord in the upper-right corner.</p>
        </Jumbotron>
        <p className="lead">
            Now supporting
            {' '}
            <a href="https://knowyourmeme.com/memes/cultures/fully-automated-luxury-gay-space-communism">
                <span className="font-family-monoton mr-1">Fully{' '}</span>
                <span className="font-family-press-start-2p">Automated</span>
                {' '}
                <span className="font-family-sail">Luxury</span>
                {' '}
                <span className="font-family-henny-penny gay-af">Gay</span>
                {' '}
                <span className="font-family-geostar">Space</span>
                {' '}
                <span className="font-family-audiowide text-danger">Communism</span>
            </a>
            !
        </p>
    </div>
};

export default hot(Splash);
