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
import {Container} from "react-bootstrap";

const Splash: React.FC = () => {
    return <div>
        <Container className="text-center">
            <p className="lead">
                Welcome to <span className="font-family-audiowide">OurTwobe</span>,
                the place to share your tunes, jokes, and other miscellaneous YouTube content!
            </p>

            <p>To get started, log in to Discord in the upper-right corner.</p>
        </Container>
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
    </div>;
};

export default Splash;
