import React, { useState, useEffect, useContext, useCallback } from "react";
import { Typography, Grid, Container, Paper } from '@material-ui/core';

import { ConfigContext } from "../config";
import { InternalLink, DataTable, Spinner } from "../component";
import { WorkflowDefinition } from "../types";
import { listWorkflowDefinitions } from "../service";

function WorkflowDefinitionListPage() {
  const config = useContext(ConfigContext);

  const [initialLoad, setInitialLoad] = useState<boolean>(true);
  const [definitions, setDefinitions] = useState<Array<WorkflowDefinition>>([]);

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

  const definitionTable = () => {
    const linkRender = (definition: WorkflowDefinition) => {
      const path = "/workflow-definition/" + definition.type;
      return <InternalLink to={path}>{definition.type}</InternalLink>
    }
    const columns = [
      { field: 'type', headerName: 'Type', rowRender: linkRender},
      { field: 'description', headerName: 'Description'},
    ];
    return (<DataTable rows={definitions} columns={columns} />)
  };

  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Container>
          <Typography variant="h2">Workflow definitions</Typography>
        </Container>
        {initialLoad ? <Container><Spinner /></Container> : definitionTable()}
      </Grid>
    </Grid>
  );
}

export default WorkflowDefinitionListPage;
