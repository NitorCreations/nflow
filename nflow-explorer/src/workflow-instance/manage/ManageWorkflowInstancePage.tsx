import {Grid, Typography} from '@mui/material';
import React from 'react';
import {WorkflowDefinition, WorkflowInstance} from '../../types';
import {UpdateWorkflowInstanceSignalForm} from './UpdateWorkflowInstanceSignalForm';
import {UpdateWorkflowInstanceStateVariableForm} from './UpdateWorkflowInstanceStateVariableForm';

const ManageWorkflowInstancePage = function (props: {
  instance: WorkflowInstance;
  definition: WorkflowDefinition;
}) {
  return (
    <Grid container>
      <Grid item xs={12}>
        <Typography variant="h5">Update state variable</Typography>
        <UpdateWorkflowInstanceStateVariableForm
          instance={props.instance}
          definition={props.definition}
        />
      </Grid>
      {props.definition.supportedSignals &&
        props.definition.supportedSignals.length > 0 && (
          <Grid item xs={12}>
            <Typography variant="h5">Send signal</Typography>
            <UpdateWorkflowInstanceSignalForm
              instance={props.instance}
              definition={props.definition}
            />
          </Grid>
        )}
    </Grid>
  );
};

export {ManageWorkflowInstancePage};
