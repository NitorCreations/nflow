import React, { useContext } from "react";
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Close';

import { FeedbackMessage } from "../types";
import "./Feedback.css";

function Feedback(props: {
    feedback: FeedbackMessage,
    onClose: () => any}) {
    return (<div className={"feedback " + props.feedback.severity}>
        {props.feedback.message}
        <IconButton size="small" aria-label="close" color="inherit" onClick={props.onClose}>
            <CloseIcon fontSize="small" />
        </IconButton>
    </div>)
}

const FeedbackContext = React.createContext<{addFeedback: (message: FeedbackMessage) => any}>({addFeedback: (x) => false});

const useFeedback = () => {
    return useContext(FeedbackContext);
};

export { Feedback, FeedbackContext, useFeedback };
