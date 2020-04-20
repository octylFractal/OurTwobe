import {User} from "firebase";
import {userInfo} from "../redux/reducer";
import {store} from "../redux/store";

export function pushUidToStore(user: User | null) {
    store.dispatch(user === null ? userInfo.logout() : userInfo.login(user.uid));
}
