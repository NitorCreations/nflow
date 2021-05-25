import React, { useContext } from "react";
import { useParams } from "react-router-dom";

import { ConfigContext } from "../config";

function WorkflowInstanceDetailsPage() {
  const config = useContext(ConfigContext);
  let { id } = useParams() as any;

  return <h2>Workflow instance {id}</h2>;
}

export default WorkflowInstanceDetailsPage;
