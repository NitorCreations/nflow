import React, { useContext, useCallback, useEffect, useState } from "react";
import { Typography, Grid, Container, Paper } from '@material-ui/core';

import WorkflowInstanceSearchForm from "./WorkflowInstanceSearchForm";
import { ConfigContext } from "../config";
import { InternalLink, DataTable, Spinner } from "../component";
import { formatRelativeTime, formatTimestamp } from "../utils";
import { listWorkflowDefinitions, listWorkflowInstances } from "../service";
import { WorkflowInstance } from "../types";

function WorkflowInstanceListPage() {
  const config = useContext(ConfigContext);

  const [initialLoad, setInitialLoad] = useState<boolean>(true);
  const [definitions, setDefinitions] = useState<Array<any>>([]);
  const [instances, setInstances] = useState<Array<WorkflowInstance>>([]);

  const fetchDefinitions = useCallback(() => {
    listWorkflowDefinitions(config)
      .then((data) => setDefinitions(data))
      .catch((error) => {
        // TODO error handling
        console.error("Error", error);
      })
      .finally(() => setInitialLoad(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => fetchDefinitions(), []);

  const searchInstances = useCallback((data: any) => {
    listWorkflowInstances(config, data)
      .then((data) => setInstances(data))
      .catch((error) => {
        // TODO error handling
        console.error("Error", error);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const instanceTable = () => {
    // TODO colors
    const idLinkRender = (instance: WorkflowInstance) => {
      const path = "/workflow/" + instance.id;
      return <InternalLink to={path}>{instance.id}</InternalLink>
    };
    const typeLinkRender = (instance: WorkflowInstance) => {
      const path = "/workflow/" + instance.id;
      return <InternalLink to={path}>{instance.type}</InternalLink>
    };
    const columns = [
      {field: 'id', headerName: 'Id', rowRender: idLinkRender},
      {field: 'type', headerName: 'Workflow type', rowRender: typeLinkRender},
      {field: 'state', headerName: 'State'},
      {field: 'stateText', headerName: 'State text'},
      {field: 'status', headerName: 'Status'},
      {field: 'businessKey', headerName: 'Business key'},
      {field: 'externalId', headerName: 'External id'},
      {field: 'retries', headerName: 'Retries'},
      {field: 'created', headerName: 'Created', fieldRender: formatTimestamp, tooltipRender: formatRelativeTime},
      {field: 'started', headerName: 'Started', fieldRender: formatTimestamp, tooltipRender: formatRelativeTime},
      {field: 'modified', headerName: 'Modified', fieldRender: formatTimestamp, tooltipRender: formatRelativeTime},
      {field: 'nextActivation', headerName: 'Next activation', fieldRender: formatTimestamp, tooltipRender: formatRelativeTime},
    ];
    const rowClassRender = (instance: any) => {
      switch(instance.status) {
        case 'manual': return 'danger';
        case 'finished': return 'success';
        case 'inProgress': return 'info';
      }
    };
    return <DataTable rows={instances} columns={columns} rowClassRender={rowClassRender}/>
  }

  const search = (data: any) => {
    searchInstances(data);
  };

  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Container>
          <Typography variant="h2">Search workflow instances</Typography>
          {initialLoad ? (
            <Spinner />
          ) : (
            <WorkflowInstanceSearchForm
              definitions={definitions}
              onSubmit={search}
            />
          )}
        </Container>
        {instanceTable()}
      </Grid>
    </Grid>
  );
}

export default WorkflowInstanceListPage;
