import React, {useContext} from 'react';
import IconButton from '@mui/material/IconButton';
import CloseIcon from '@mui/icons-material/Close';

import {FeedbackMessage} from '../types';
import './Feedback.scss';

function Feedback(props: {feedback: FeedbackMessage; onClose: () => any}) {
  return (
    <div className={'feedback ' + props.feedback.severity}>
      {props.feedback.message}
      <IconButton
        size="small"
        aria-label="close"
        color="inherit"
        onClick={props.onClose}
      >
        <CloseIcon fontSize="small" />
      </IconButton>
    </div>
  );
}

const FeedbackContext = React.createContext<{
  addFeedback: (message: FeedbackMessage) => any;
  getCurrentFeedback: () => FeedbackMessage | undefined;
}>({
  addFeedback: x => false,
  getCurrentFeedback: () => undefined
});

const useFeedback = () => {
  return useContext(FeedbackContext);
};

export {Feedback, FeedbackContext, useFeedback};
