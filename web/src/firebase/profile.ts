import {state$, store} from "../redux/store";
import {distinct, map, switchMap} from "rxjs/operators";
import {firebaseApp} from "./setup";
import {doc} from "rxfire/firestore";
import {userInfo} from "../redux/reducer";
import {net} from "common";
import {of} from "rxjs";
import UserProfile = net.octyl.ourtwobe.UserProfile;

export function setupProfileHook() {
    const collection = firebaseApp.firestore().collection("profiles")
    state$.pipe(
        map(value => value.userInfo.uid),
        distinct(),
        switchMap(uid => typeof uid === "undefined"
            ? of(undefined)
            : doc(collection.doc(uid)).pipe(
                map(snapshot => snapshot.data() as UserProfile)
            )),
    ).subscribe(profile => store.dispatch(userInfo.setProfile(profile)))
}
