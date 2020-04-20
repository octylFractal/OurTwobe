import React from "react";
import classNames from "classnames";

export const NavbarImg: React.FC<JSX.IntrinsicElements["img"]> = (props) => {
    return <img {...props} alt={props.alt} width={48} height={48}
                className={classNames(props.className, "navbar-img")}/>;
};
