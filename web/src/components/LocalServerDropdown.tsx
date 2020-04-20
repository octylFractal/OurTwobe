import {connect} from "react-redux";
import {LocalState} from "../redux/store";
import {ServerDropdown} from "./ServerDropdown";

export const LocalServerDropdown = connect((state: LocalState) => ({
    loggedIn: typeof state.userInfo.uid !== "undefined",
    servers: state.userInfo.profile?.servers ?? []
}))(ServerDropdown);
