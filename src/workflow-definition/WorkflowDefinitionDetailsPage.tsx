import React from "react";
import { Link, useParams } from "react-router-dom";

function WorkflowDefinitionDetailsPage() {
  let { id } = useParams() as any;

  // TODO required features
  // description
  // search related instances link: DONE
  // all instances summary table
  // workflow settings
  // state graph

  // TODO new features
  // launch a new instance

  // TODO skipped features
  // radiator
  const type = "creditDecision";
  const searchPath = "/search?type=" + type;

  return (
    <div>
      <h1>Workflow definition details for id {id}</h1>

      <Link to={searchPath}>Search related workflows</Link>
    </div>
  );
}

export default WorkflowDefinitionDetailsPage;
