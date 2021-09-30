import {Grid, Typography} from '@material-ui/core';
import React from 'react';
import {WorkflowDefinition, WorkflowInstance} from '../../types';
import {UpdateWorkflowInstanceStateForm} from './UpdateWorkflowInstanceStateForm';
import {UpdateWorkflowInstanceStateVariableForm} from './UpdateWorkflowInstanceStateVariableForm';

const ManageWorkflowInstancePage = function (props: {
  instance: WorkflowInstance;
  definition: WorkflowDefinition;
}) {
  return (
    <Grid container>
      <Grid item xs={12}>
        <Typography variant="h3">Update state</Typography>
        <UpdateWorkflowInstanceStateForm
          instance={props.instance}
          definition={props.definition}
        />
      </Grid>
      <Grid item xs={12}>
        <Typography variant="h3">Update state variable</Typography>
        <UpdateWorkflowInstanceStateVariableForm
          instance={props.instance}
          definition={props.definition}
        />
      </Grid>
    </Grid>
  );
};

export {ManageWorkflowInstancePage};
