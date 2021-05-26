import React, { useEffect, useState, useContext } from "react";
import { useParams } from "react-router-dom";

import { StateGraph, InternalLink, ObjectTable, Spinner } from "../component";
import { WorkflowDefinition, WorkflowInstance } from "../types";
import { ConfigContext } from "../config";
import { getWorkflowDefinition, getWorkflowInstance} from "../service";
import { formatTimestamp, formatRelativeTime } from "../utils";
import { StateVariableTable } from "./StateVariableTable";

function WorkflowInstanceDetailsPage() {
  const config = useContext(ConfigContext);

  const [definition, setDefinition] = useState<WorkflowDefinition>();
  const [instance, setInstance] = useState<WorkflowInstance>();
  const [parentInstance, setParentInstance] = useState<WorkflowInstance>();
  const { id } = useParams() as any;

  useEffect(() => {
    getWorkflowInstance(config, id)
      .then(workflow => {
        if (workflow.parentWorkflowId) {
          return Promise.all([
            getWorkflowInstance(config, workflow.parentWorkflowId),
            getWorkflowDefinition(config, workflow.type)
          ])
            .then(([parent, definition]) => {
              setDefinition(definition);
              setParentInstance(parent);
              setInstance(workflow);
            })
        }
        return getWorkflowDefinition(config, workflow.type)
          .then(definition => {
            setDefinition(definition);
            setParentInstance(undefined);
            setInstance(workflow);
          });
      })
  }, [config, id]);

  const instanceSummaryTable = (instance: WorkflowInstance, parentInstance?: WorkflowInstance) => {
    // TODO childWorkflows
    const parentLink = (x: any) => parentInstance && <InternalLink to={"/workflow/" + parentInstance.id}>{parentInstance.type} ({parentInstance.id})</InternalLink>;
    const columns = [
      {field: 'parentWorkflowId', headerName: 'Parent workflow', fieldRender: parentLink},
      {field: 'state', headerName: 'Current state'},
      {field: 'status', headerName: 'Current status'},
      {field: 'nextActivation', headerName: 'Next activation', fieldRender: formatRelativeTime, tooltipRender: formatTimestamp},
      {field: 'businessKey', headerName: 'Business key'},
      {field: 'externalId', headerName: 'External id'},
      {field: 'created', headerName: 'Created', fieldRender: formatTimestamp, tooltipRender: formatRelativeTime},
      {field: 'started', headerName: 'Started', fieldRender: formatTimestamp, tooltipRender: formatRelativeTime},
      {field: 'modified', headerName: 'Modified', fieldRender: formatTimestamp, tooltipRender: formatRelativeTime},
    ]
    return <ObjectTable object={instance} columns={columns} />
  };

  // TODO if the workflow is active, re-fetch peridodically
  // - status executing
  // - status create/inProgress and has nextActivation (which is less than 24h away?)
  const instanceSummary = (definition: WorkflowDefinition, instance: WorkflowInstance, parentInstance?: WorkflowInstance) => {
    return (
      <div>
        <h2><InternalLink to={"/workflow-definition/" + instance.type}>{instance.type}</InternalLink> ({instance.id})</h2>
        {instanceSummaryTable(instance, parentInstance)}
        <StateVariableTable instance={instance} />
        <StateGraph definition={definition} />
      </div>
      )
  };

  return (
    <div>
      { (definition && instance) ? instanceSummary(definition, instance, parentInstance) : <Spinner />}
    </div>
  );
}

export default WorkflowInstanceDetailsPage;
