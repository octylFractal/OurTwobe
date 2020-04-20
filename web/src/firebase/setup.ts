import firebase from "firebase/app";
import "firebase/analytics";
import "firebase/auth";
import "firebase/firestore";
import {pushUidToStore} from "./redux-init";
import {setupProfileHook} from "./profile";

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
firebaseApp.firestore().enablePersistence()
    .catch(function (err) {
        if (err.code == 'failed-precondition') {
            alert("You should only keep one tab of OurTwobe open.");
            return;
        } else if (err.code == 'unimplemented') {
            return;
        } else {
            console.warn("Failed to enable persistence", err.code)
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

setupProfileHook()

