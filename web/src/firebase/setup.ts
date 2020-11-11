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

import firebase from "firebase/app";
import "firebase/analytics";
import "firebase/auth";
import "firebase/firestore";
import {pushUidToStore} from "./redux-init";

const firebaseConfig = {
    apiKey: "AIzaSyDQ5-304MuuKVxSbuqhNC_sBGNCkDEpLHY",
    authDomain: "ourtube-2.firebaseapp.com",
    databaseURL: "https://ourtube-2.firebaseio.com",
    projectId: "ourtube-2",
    storageBucket: "ourtube-2.appspot.com",
    messagingSenderId: "1034173251909",
    appId: "1:1034173251909:web:b1f09dd5c55b5c488dfe2c"
};

export const firebaseApp = firebase.initializeApp(firebaseConfig);
if (process.env.NODE_ENV !== 'production') {
    firebaseApp.firestore().settings({
        host: "localhost:10052",
        ssl: false,
    });
}
firebaseApp.firestore().enablePersistence()
    .catch(function (err) {
        if (err.code == 'failed-precondition') {
            alert("You should only keep one tab of OurTwobe open.");
            return;
        } else if (err.code == 'unimplemented') {
            return;
        } else {
            console.warn("Failed to enable persistence", err.code);
        }
    });

const auth = firebaseApp.auth();
auth.onIdTokenChanged(pushUidToStore, error => console.log(error));
auth.getRedirectResult()
    .then(creds => {
        if (creds.user === null) {
            return;
        }
        return auth.updateCurrentUser(creds.user);
    })
    .catch(err => {
        console.info(err);
    });

