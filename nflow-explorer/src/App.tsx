import React, {useState} from 'react';
import './App.scss';
import {Navigate, HashRouter as Router, Route, Routes} from 'react-router-dom';
import {Snackbar} from '@mui/material';

import {Navigation, Feedback, FeedbackContext} from './component';
import {FeedbackMessage} from './types';

// TODO get rid of default exports in Pages
import WorkflowDefinitionListPage from './workflow-definition/WorkflowDefinitionListPage';
import WorkflowDefinitionDetailsPage from './workflow-definition/WorkflowDefinitionDetailsPage';

import WorkflowInstanceListPage from './workflow-instance/WorkflowInstanceListPage';
import WorkflowInstanceDetailsPage from './workflow-instance/WorkflowInstanceDetailsPage';
import {CreateWorkflowInstancePage} from './workflow-instance/CreateWorkflowInstancePage';

import ExecutorListPage from './executor/ExecutorListPage';

import AboutPage from './about/AboutPage';
import NotFoundPage from './error/NotFoundPage';
import {ReturnLink} from './component/ReturnLink';

function App() {
  const [feedback, setFeedback] = useState<FeedbackMessage | undefined>();

  const addFeedback = (feedback: FeedbackMessage) => {
    setFeedback(feedback);
  };

  const getCurrentFeedback = () => {
    return feedback;
  };

  const closeFeedback = () => {
    setFeedback(undefined);
  };

  return (
    <Router>
      <FeedbackContext.Provider value={{addFeedback, getCurrentFeedback}}>
        <div className="App">
          <header>
            <ReturnLink />
            <Navigation />
          </header>
          <div className="content-area">
            <Routes>
              <Route path="/" element={<Navigate to="/workflow" />} />
              <Route path="/search" element={<Navigate to="/workflow" />} />
              <Route
                path="/workflow/create"
                element={<CreateWorkflowInstancePage />}
              />
              <Route
                path="/workflow/:id"
                element={<WorkflowInstanceDetailsPage />}
              />
              <Route path="/workflow" element={<WorkflowInstanceListPage />} />
              <Route
                path="/workflow-definition/:type"
                element={<WorkflowDefinitionDetailsPage />}
              />
              <Route
                path="/workflow-definition"
                element={<WorkflowDefinitionListPage />}
              />
              <Route path="/executors" element={<ExecutorListPage />} />
              <Route path="/about" element={<AboutPage />} />
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </div>

          <Snackbar
            open={!!feedback}
            autoHideDuration={10000}
            anchorOrigin={{
              vertical: 'top',
              horizontal: 'right'
            }}
            onClose={closeFeedback}
          >
            <div>
              {feedback && (
                <Feedback feedback={feedback} onClose={closeFeedback} />
              )}
            </div>
          </Snackbar>
        </div>
      </FeedbackContext.Provider>
    </Router>
  );
}

export default App;
