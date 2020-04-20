import {connect} from "react-redux";
import {LocalState} from "../redux/store";
import {UserState} from "./UserState";

export const LocalUserState = connect((state: LocalState) => ({
    userInfo: state.userInfo
}))(UserState);
