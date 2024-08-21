import React, {useCallback, useEffect, useState} from 'react';
import {useLocation, useHistory} from 'react-router-dom';
import {Box, Grid, TextField} from '@material-ui/core';
import Button from '@material-ui/core/Button';
import {makeStyles} from '@material-ui/core/styles';

import {Selection} from '../component';
import {Executor} from '../types';

const useStyles = makeStyles(theme => ({
  formControl: {
    margin: theme.spacing(1),
    minWidth: 200,
    maxWidth: 500
  },
  root: {
    '& .MuiTextField-root': {
      margin: theme.spacing(1)
    }
  }
}));

const allMarker = '!-all-!';

const statuses = [
  allMarker,
  'created',
  'inProgress',
  'finished',
  'manual',
  'executing'
];

const typeNames: any = {
  [allMarker]: '-- All types --'
};

const stateNames: any = {
  [allMarker]: '-- All states --'
};

const statusNames: any = {
  [allMarker]: '-- All statuses --'
};
const executorGroupNames: any = {
  [allMarker]: '-- Default --'
};

function WorkflowInstanceSearchForm(props: {
  definitions: Array<any>;
  executorGroups: Array<any>;
  onSubmit: (data: any) => any;
}) {
  const classes = useStyles();
  const history = useHistory();

  const queryParams = new URLSearchParams(useLocation().search);

  const [type, setType] = useState<string>(
    queryParams.get('type') || allMarker
  );
  const [state, setState] = useState<string>(
    queryParams.get('state') || allMarker
  );
  const [status, setStatus] = useState<string>(
    queryParams.get('status') || allMarker
  );
  const [executorGroup, setExecutorGroup] = useState<string>(
    queryParams.get('executorGroups') || allMarker
  );
  const [businessKey, setBusinessKey] = useState<string>(
    queryParams.get('businessKey') || ''
  );
  const [externalId, setExternalId] = useState<string>(
    queryParams.get('externalId') || ''
  );
  const [id, setId] = useState<string>(queryParams.get('id') || '');
  const [parentWorkflowId, setParentWorkflowId] = useState<string>(
    queryParams.get('parentWorkflowId') || ''
  );

  const types = [allMarker].concat(props.definitions.map(d => d.type));

  //concat the executor groups
  // and filter where expires greater than now and not stopped
  const executorGroups = [allMarker].concat(
    props.executorGroups
      .map((executorGroup: Executor) => executorGroup.executorGroup)
      //remove duplicates
      .filter((value, index, self) => self.indexOf(value) === index)
  );

  // TODO to lodash or not to lodash?
  const selectedWorkflow = props.definitions.filter(d => d.type === type)[0];
  const selectedStates = (
    (selectedWorkflow && selectedWorkflow.states) ||
    []
  ).map((s: any) => s.id);

  const states = [allMarker].concat(selectedStates);

  const handleSubmit = useCallback(
    (e?: any) => {
      e && e.preventDefault();
      const data: any = {
        type,
        state,
        status,
        executorGroup,
        businessKey,
        externalId,
        id,
        parentWorkflowId
      };

      for (let field of Object.keys(data)) {
        if (data[field]) {
          data[field] = data[field].trim();
        }
        if (data[field] === allMarker) {
          data[field] = undefined;
        }
      }
      // remove empty fields, convert ids to numbers
      for (let field of ['id', 'parentWorkflowId']) {
        if (field in data) {
          data[field] = parseInt(data[field], 10) || undefined;
        }
      }
      for (let k of Object.keys(data)) {
        if (data[k] === '' || Number.isNaN(data[k]) || data[k] === undefined) {
          delete data[k];
        }
      }
      // Update query parameters to URL
      if (Object.values(data).every(x => !x)) {
        data.emptyCriteria = true;
      }
      history.push(`/workflow?${new URLSearchParams(data).toString()}`);
      props.onSubmit(data);
    },
    [
      businessKey,
      externalId,
      history,
      id,
      parentWorkflowId,
      props,
      state,
      executorGroup,
      status,
      type
    ]
  );

  // if query parameters are give, trigger immediate search
  useEffect(() => {
    if (queryParams.keys().next()?.value) {
      handleSubmit();
    }
    // TODO: adding proper deps here causes a render loop. This works ok for now but it should be fixed
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const setWorkflowType = (type: string) => {
    // when type is changed, need to reset all selections that depend on type
    setType(type);
    setExecutorGroup(executorGroup);
    setState(allMarker);
  };

  return (
    <form className={classes.root}>
      <Grid container alignItems="center">
        <Grid item xs={11}>
          <Selection
            label="Type"
            items={types}
            selected={type}
            onChange={setWorkflowType}
            getSelectionLabel={(type: string) => typeNames[type] || type}
          />

          <Selection
            label="State"
            items={states}
            selected={state}
            onChange={setState}
            getSelectionLabel={(state: string) => stateNames[state] || state}
          />

          <Selection
            label="Status"
            items={statuses}
            selected={status}
            onChange={setStatus}
            getSelectionLabel={(status: string) =>
              statusNames[status] || status
            }
          />
          <Selection
            label="Executor Group"
            items={executorGroups}
            selected={executorGroup}
            onChange={setExecutorGroup}
            getSelectionLabel={(executorGroup: string) =>
              executorGroupNames[executorGroup] || executorGroup
            }
          />

          <TextField
            label="Business key"
            value={businessKey}
            onChange={e => setBusinessKey(e.target.value)}
          />

          <TextField
            label="External ID"
            value={externalId}
            onChange={e => setExternalId(e.target.value)}
          />

          <TextField
            label="Instance ID"
            type="number"
            value={id}
            onChange={e => setId(e.target.value)}
          />

          <TextField
            label="Parent instance ID"
            type="number"
            value={parentWorkflowId}
            onChange={e => setParentWorkflowId(e.target.value)}
          />
        </Grid>

        <Grid item xs={1}>
          <Box display="flex" flexDirection="column">
            <Button onClick={handleSubmit} variant="contained">
              Search
            </Button>
          </Box>
        </Grid>
      </Grid>
    </form>
  );
}

export default WorkflowInstanceSearchForm;
