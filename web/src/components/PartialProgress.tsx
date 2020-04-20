import React from "react";
import {Progress} from "reactstrap";

export interface PartialProgressProps {
    percentage: number;
    start: number;
    cap: number;
    color: string;
}

export const PartialProgress: React.FC<PartialProgressProps> = ({percentage, start, cap, color}) => {
    if (percentage >= start) {
        const realPercent = Math.min(cap, percentage);
        return <Progress animated bar color={color} value={realPercent - start}/>;
    }
    return null;
};