import {Store} from "redux";
import {Observable} from "rxjs";
import {Selector} from "../redux/selectors";

export function observeStore<S, R>(store: Store<S>, selector: Selector<S, R>): Observable<R> {
    return new Observable<R>(subscriber => {
        return store.subscribe(() => {
            try {
                const value = selector(store.getState());
                if (selector.recomputations() > 0) {
                    selector.resetRecomputations();
                    subscriber.next(value);
                }
            } catch (e) {
                subscriber.error(e);
            }
        });
    });
}
