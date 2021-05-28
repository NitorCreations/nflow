import React, { Fragment, useEffect, useState, useContext } from "react";
import { useParams } from "react-router-dom";
import { Typography, Grid, Container } from '@material-ui/core';

import { StateGraph, InternalLink, ObjectTable, Spinner } from "../component";
import { WorkflowDefinition, WorkflowInstance } from "../types";
import { ConfigContext } from "../config";
import { getWorkflowDefinition, getWorkflowInstance, listChildWorkflowInstances} from "../service";
import { formatTimestamp, formatRelativeTime } from "../utils";
import { StateVariableTable } from "./StateVariableTable";
import { ActionHistoryTable } from "./ActionHistoryTable";

function WorkflowInstanceDetailsPage() {
  const config = useContext(ConfigContext);

  const [definition, setDefinition] = useState<WorkflowDefinition>();
  const [instance, setInstance] = useState<WorkflowInstance>();
  const [childInstances, setChildInstances] = useState<WorkflowInstance[]>([])
  const [parentInstance, setParentInstance] = useState<WorkflowInstance>();
  const { id } = useParams() as any;

  // TODO This fetches childWorkflows for ActionHistoryTable
  //      There may be a lot of child workflows => possible performance problem
  useEffect(() => {
    Promise.all([getWorkflowInstance(config, id),
                listChildWorkflowInstances(config, id)])
      .then(([instance, childInstances]) => {
        if (instance.parentWorkflowId) {
          return Promise.all([
            getWorkflowInstance(config, instance.parentWorkflowId),
            getWorkflowDefinition(config, instance.type)
          ])
            .then(([parent, definition]) => {
              setDefinition(definition);
              setParentInstance(parent);
              setInstance(instance);
              setChildInstances(childInstances)
            })
        }
        return getWorkflowDefinition(config, instance.type)
          .then(definition => {
            setDefinition(definition);
            setParentInstance(undefined);
            setInstance(instance);
            setChildInstances(childInstances);
          });
      })
  }, [config, id]);

  const instanceSummaryTable = (instance: WorkflowInstance, parentInstance?: WorkflowInstance) => {
    // TODO clicking currentState should highlight in state graph
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
    ];

    const valueClassRender = (column: any, instance: any) => {
      if (column.field !== 'status') {
        return '';
      }
      switch(instance.status) {
        case 'manual': return 'danger';
        case 'finished': return 'success';
        case 'inProgress': return 'info';
      }
    };
    return <ObjectTable object={instance} columns={columns} valueClassRender={valueClassRender}/>
  };

  // TODO if the workflow is active, re-fetch peridodically
  // - status executing
  // - status create/inProgress and has nextActivation (which is less than 24h away?)
  const instanceSummary = (definition: WorkflowDefinition, instance: WorkflowInstance, parentInstance?: WorkflowInstance) => {
    return (
      <Fragment>
        <Grid item xs={12} sm={6}>
          <Container>
            <Typography variant="h2">
              <InternalLink to={"/workflow-definition/" + instance.type}>{instance.type}</InternalLink> ({instance.id})
            </Typography>
            {instanceSummaryTable(instance, parentInstance)}
          </Container>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Container>
            <Typography variant="h3">State variables</Typography>
            <StateVariableTable instance={instance} />
          </Container>
          <Container>
            <Typography variant="h3">Action history</Typography>
            <ActionHistoryTable instance={instance} childInstances={childInstances} />
          </Container>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Container>
            <StateGraph definition={definition} />
          </Container>
        </Grid>
      </Fragment>
    );
  };

  return (
    <Grid container spacing={3}>
        { (definition && instance) ? instanceSummary(definition, instance, parentInstance) : 
        <Grid item xs={12}>
          <Container>
            <Spinner />
          </Container>
        </Grid>}
    </Grid>
  );
}

export default WorkflowInstanceDetailsPage;
