import {Button, FormLabel, Grid, MenuItem} from '@material-ui/core';
import {TextField, Select} from 'mui-rff';
import React, {useContext, useState} from 'react';
import {Form} from 'react-final-form';
import {ConfigContext} from '../../config';

import {
  WorkflowDefinition,
  WorkflowInstance,
  WorkflowSignal
} from '../../types';
import {sendWorkflowInstanceSignal} from '../../service';
import {Alert} from '@material-ui/lab';

const UpdateWorkflowInstanceSignalForm = function (props: {
  instance: WorkflowInstance;
  definition: WorkflowDefinition;
}) {
  const config = useContext(ConfigContext);
  const [errorMsg, setErrorMsg] = useState('');
  const [confirmationMsg, setConfirmationMsg] = useState('');

  const onSubmit = (values: any) => {
    return sendWorkflowInstanceSignal(config, props.instance.id, {
      signal: values.signal,
      reason: values.reason
    }).then((reason: any) => {
      if (reason.error) {
        setErrorMsg(reason.error);
        setConfirmationMsg('');
      } else {
        setErrorMsg('');
        setConfirmationMsg(`Signal updated to ${values.signal}`);
      }
    });
  };

  return (
    <Form
      onSubmit={onSubmit}
      initialValues={{
        signal: props.instance.state
      }}
      render={({handleSubmit, submitting}) => (
        <form onSubmit={handleSubmit}>
          <Grid container spacing={1} alignItems="center">
            <Grid item xs={4}>
              <FormLabel>Send signal</FormLabel>
            </Grid>
            <Grid item xs={8}>
              <Select name="signal">
                {props.definition.supportedSignals.map(
                  (signal: WorkflowSignal) => {
                    return (
                      <MenuItem key={signal.value} value={signal.value}>
                        {signal.description} ({signal.value})
                      </MenuItem>
                    );
                  }
                )}
              </Select>
            </Grid>
            <Grid item xs={12}>
              <TextField label="Signal reason" name="reason" />
            </Grid>
            <Grid item xs={12}>
              <Button type="submit" variant="contained" disabled={submitting}>
                Send
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

export {UpdateWorkflowInstanceSignalForm};
