import React, { useEffect, useState, useContext } from "react";
import { Link, useParams } from "react-router-dom";

import { Spinner } from "../component";
import { WorkflowInstance } from "../types";
import { ConfigContext } from "../config";
import { getWorkflowInstance} from "../service";
import { formatTimestamp } from "../utils";

function WorkflowInstanceDetailsPage() {
  const config = useContext(ConfigContext);

  const [instance, setInstance] = useState<WorkflowInstance>();
  const [parentInstance, setParentInstance] = useState<WorkflowInstance>();
  const { id } = useParams() as any;

  useEffect(() => {
    getWorkflowInstance(config, id)
      .then(workflow => {
        if (workflow.parentWorkflowId) {
          return getWorkflowInstance(config, workflow.parentWorkflowId)
            .then(parent => {
              setParentInstance(parent);
              setInstance(workflow);
            })
        }
        setParentInstance(undefined);
        setInstance(workflow)
      })
  }, [config, id]);

  // TODO if the workflow is active, re-fetch peridodically
  // - status executing
  // - status create/inProgress and has nextActivation (which is less than 24h away?)
  const instanceSummary = (instance: WorkflowInstance, parentInstance?: WorkflowInstance) => {
    return (
      <div>
      <h2><Link to={"/workflow-definition/" + instance.type}>{instance.type}</Link> ({instance.id})</h2>
      <table>
        <tbody>
          {parentInstance &&
            <tr>
              <td>Parent workflow</td>
              <td><Link to={"/workflow/" + parentInstance.id}>{parentInstance.type} ({parentInstance.id})</Link></td>
            </tr>
          }
          <tr><td>Current state</td><td>{instance.state}</td></tr>
          <tr><td>Current status</td><td>{instance.status}</td></tr>
          <tr><td>Next activation</td><td>{formatTimestamp(instance.nextActivation) || 'never'}</td></tr>
          {instance.businessKey &&
            <tr><td>Business key</td><td><strong>{instance.businessKey}</strong></td></tr>}
          <tr><td>External id</td><td><strong>{instance.externalId}</strong></td></tr>
          <tr><td>Created</td><td>{formatTimestamp(instance.created)}</td></tr>
          <tr><td>Started</td><td>{formatTimestamp(instance.started)}</td></tr>
          <tr><td>Modified</td><td>{formatTimestamp(instance.modified)}</td></tr>
        </tbody>
      </table>
      </div>
      )
  };
  return (
    <div>
      { instance ? instanceSummary(instance, parentInstance) : <Spinner />}
    </div>
  );
}

export default WorkflowInstanceDetailsPage;
