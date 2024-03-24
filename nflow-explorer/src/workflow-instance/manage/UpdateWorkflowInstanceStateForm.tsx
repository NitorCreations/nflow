import {Button, FormLabel, Grid, MenuItem} from '@material-ui/core';
import {TextField, Select} from 'mui-rff';
import React, {useContext, useState} from 'react';
import {Form} from 'react-final-form';
import {ConfigContext} from '../../config';

import {WorkflowDefinition, WorkflowInstance, WorkflowState} from '../../types';
import {updateWorkflowInstance} from '../../service';
import moment from 'moment';
import {Alert} from '@material-ui/lab';

const UpdateWorkflowInstanceStateForm = function (props: {
  instance: WorkflowInstance;
  definition: WorkflowDefinition;
}) {
  const config = useContext(ConfigContext);
  const [errorMsg, setErrorMsg] = useState('');
  const [confirmationMsg, setConfirmationMsg] = useState('');

  const onSubmit = (values: any) => {
    const now = moment(new Date());
    return updateWorkflowInstance(config, props.instance.id, {
      state: values.nextState,
      nextActivationTime: now.add(
        moment.duration(values.duration, values.timeUnit)
      ),
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
        setConfirmationMsg(`State updated to ${values.nextState}`);
      }
    });
  };

  return (
    <Form
      onSubmit={onSubmit}
      initialValues={{
        nextState: props.instance.state,
        duration: 0,
        timeUnit: 'hours'
      }}
      render={({handleSubmit, submitting}) => (
        <form onSubmit={handleSubmit}>
          <Grid container spacing={1} alignItems="center">
            <Grid item xs={4}>
              <FormLabel>Set state to</FormLabel>
            </Grid>
            <Grid item xs={8}>
              <Select name="nextState">
                {props.definition.states.map((state: WorkflowState) => {
                  return (
                    <MenuItem key={state.id} value={state.id}>
                      {state.id}
                    </MenuItem>
                  );
                })}
              </Select>
            </Grid>
            <Grid item xs={4}>
              <FormLabel>Next activation in</FormLabel>
            </Grid>
            <Grid item xs={4}>
              <TextField name="duration" type="number"></TextField>
            </Grid>
            <Grid item xs={4}>
              <Select name="timeUnit">
                <MenuItem value="minutes">minutes</MenuItem>
                <MenuItem value="hours">hours</MenuItem>
                <MenuItem value="days">days</MenuItem>
              </Select>
            </Grid>
            <Grid item xs={12}>
              <TextField label="Action description" name="actionDescription" />
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

export {UpdateWorkflowInstanceStateForm};
