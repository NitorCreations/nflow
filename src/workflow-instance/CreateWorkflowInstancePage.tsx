import React, {useEffect, useState} from 'react';
import {Typography, Grid, Container} from '@material-ui/core';

import {CreateWorkflowInstanceForm} from './CreateWorkflowInstanceForm';
import {WorkflowDefinition} from '../types';
import {useConfig} from '../config';
import {Spinner} from '../component';
import {listWorkflowDefinitions} from '../service';

const ShowForm = ({definitions}: {definitions: WorkflowDefinition[]}) => {
  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Container>
          <Typography variant="h2" gutterBottom>
            Create a new workflow instance
          </Typography>
          <CreateWorkflowInstanceForm definitions={definitions} />
        </Container>
      </Grid>
    </Grid>
  );
};

function CreateWorkflowInstancePage() {
  const config = useConfig();

  const [definitions, setDefinitions] = useState<WorkflowDefinition[]>();

  useEffect(() => {
    listWorkflowDefinitions(config).then(setDefinitions);
  }, [config]);

  if (definitions) {
    return <ShowForm definitions={definitions} />;
  }

  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Container>
          <Spinner />
        </Container>
      </Grid>
    </Grid>
  );
}

export {CreateWorkflowInstancePage};
