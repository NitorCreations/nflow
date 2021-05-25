import React from "react";
import "./App.css";
import { BrowserRouter as Router, Switch, Route, Link } from "react-router-dom";

import WorkflowDefinitionListPage from "./workflow-definition/WorkflowDefinitionListPage";
import WorkflowDefinitionDetailsPage from "./workflow-definition/WorkflowDefinitionDetailsPage";

import WorkflowInstanceListPage from "./workflow-instance/WorkflowInstanceListPage";
import WorkflowInstanceDetailsPage from "./workflow-instance/WorkflowInstanceDetailsPage";

import ExecutorListPage from "./executor/ExecutorListPage";

import AboutPage from "./about/AboutPage";

import NotFoundPage from "./error/NotFoundPage";

function App() {
  return (
    <Router>
      <div className="App">
        <header className="App-header">
          <Link to="/">
            <img src="/nflow_logo.svg" alt="nFlow-lo" />
          </Link>
          <Link to="/">Workflow definitions</Link>
          <Link to="/search">Workflow instances</Link>
          <Link to="/executors">Executors</Link>
          <Link to="/about">About</Link>
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
