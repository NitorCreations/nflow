import {Button, Grid} from '@mui/material';
import {TextField} from 'mui-rff';
import React, {useContext, useState} from 'react';
import {Form} from 'react-final-form';
import {ConfigContext} from '../../config';

import {WorkflowDefinition, WorkflowInstance} from '../../types';
import {updateWorkflowInstance} from '../../service';
import {Alert} from '@mui/material';

const UpdateWorkflowInstanceStateVariableForm = function (props: {
  instance: WorkflowInstance;
  definition: WorkflowDefinition;
}) {
  const config = useContext(ConfigContext);
  const [errorMsg, setErrorMsg] = useState('');
  const [confirmationMsg, setConfirmationMsg] = useState('');

  const onSubmit = (values: any) => {
    return updateWorkflowInstance(config, props.instance.id, {
      stateVariables: {
        [values.variableName]: values.variableValue
      },
      actionDescription: values.actionDescription
    }).then((reason: any) => {
      if (!reason.ok) {
        setErrorMsg(`HTTP error! status: ${reason.status}`);
        setConfirmationMsg('');
      } else if (reason.error) {
        setErrorMsg(reason.error);
        setConfirmationMsg('');
      } else {
        setErrorMsg('');
        setConfirmationMsg(`State variable ${values.variableName} updated`);
      }
    });
  };

  return (
    <Form
      onSubmit={onSubmit}
      render={({handleSubmit, submitting}) => (
        <form onSubmit={handleSubmit}>
          <Grid container spacing={1} alignItems="center">
            <Grid item xs={12}>
              <TextField
                label="State variable name"
                name="variableName"
                required={true}
                variant="standard"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="State variable value"
                name="variableValue"
                variant="outlined"
                multiline={true}
                minRows={4}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField label="Action description" name="actionDescription" variant="standard"/>
            </Grid>
            <Grid item xs={12}>
              <Button type="submit" variant="contained" disabled={submitting}>
                Update
              </Button>
            </Grid>
            <Grid item xs={12}>
              {errorMsg && <Alert severity="error">{errorMsg}</Alert>}
              {confirmationMsg && (
                <Alert severity="success">{confirmationMsg}</Alert>
              )}
            </Grid>
          </Grid>
        </form>
      )}
    />
  );
};

export {UpdateWorkflowInstanceStateVariableForm};
