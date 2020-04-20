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
