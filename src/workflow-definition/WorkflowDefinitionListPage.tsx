import React, {useState, useEffect, useCallback} from 'react';
import {Grid, Container} from '@material-ui/core';

import {useConfig} from '../config';
import {InternalLink, DataTable, Spinner} from '../component';
import {WorkflowDefinition} from '../types';
import {listWorkflowDefinitions} from '../service';

const DefinitionTable = ({
  definitions
}: {
  definitions: WorkflowDefinition[];
}) => {
  const linkRender = (definition: WorkflowDefinition) => {
    const path = '/workflow-definition/' + definition.type;
    return <InternalLink to={path}>{definition.type}</InternalLink>;
  };

  const columns = [
    {field: 'type', headerName: 'Type', rowRender: linkRender},
    {field: 'description', headerName: 'Description'}
  ];

  return <DataTable rows={definitions} columns={columns} />;
};

function WorkflowDefinitionListPage() {
  const config = useConfig();

  const [initialLoad, setInitialLoad] = useState<boolean>(true);
  const [definitions, setDefinitions] = useState<Array<WorkflowDefinition>>([]);

  const fetchDefinitions = useCallback(() => {
    listWorkflowDefinitions(config)
      .then(data => setDefinitions(data))
      .catch(error => {
        // TODO error handling
        console.error('Error', error);
      })
      .finally(() => setInitialLoad(false));
  }, [config]);

  useEffect(() => fetchDefinitions(), [fetchDefinitions]);

  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        {initialLoad ? (
          <Container>
            <Spinner />
          </Container>
        ) : (
          <DefinitionTable definitions={definitions} />
        )}
      </Grid>
    </Grid>
  );
}

export default WorkflowDefinitionListPage;
