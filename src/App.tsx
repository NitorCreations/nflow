import React from "react";
import "./App.css";
import { BrowserRouter as Router, Switch, Route } from "react-router-dom";

import WorkflowDefinitionListPage from "./workflow-definition/WorkflowDefinitionListPage";
import WorkflowDefinitionDetailsPage from "./workflow-definition/WorkflowDefinitionDetailsPage";

import WorkflowInstanceListPage from "./workflow-instance/WorkflowInstanceListPage";
import WorkflowInstanceDetailsPage from "./workflow-instance/WorkflowInstanceDetailsPage";

import ExecutorListPage from "./executor/ExecutorListPage";

import AboutPage from "./about/AboutPage";
import { Navigation } from "./component";
import NotFoundPage from "./error/NotFoundPage";

function App() {
  return (
    <Router>
      <div className="App">
        <header>
          <Navigation />
        </header>
        <hr />
        <Switch>
          <Route exact path="/">
            <WorkflowDefinitionListPage />
          </Route>
          <Route path="/search">
            <WorkflowInstanceListPage />
          </Route>
          <Route path="/workflow-definition/:type">
            <WorkflowDefinitionDetailsPage />
          </Route>
          <Route path="/workflow/:id">
            <WorkflowInstanceDetailsPage />
          </Route>
          <Route path="/executors">
            <ExecutorListPage />
          </Route>
          <Route path="/about">
            <AboutPage />
          </Route>

          <Route path="*">
            <NotFoundPage />
          </Route>
        </Switch>
      </div>
    </Router>
  );
}

export default App;
