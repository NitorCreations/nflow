import React, {useCallback, useEffect, useState} from 'react';
import {useLocation, useHistory} from 'react-router-dom';
import TextField from '@material-ui/core/TextField';
import Button from '@material-ui/core/Button';
import {makeStyles} from '@material-ui/core/styles';

import {Selection} from '../component';

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
  [allMarker]: '-- All workflow types --'
};

const stateNames: any = {
  [allMarker]: '-- All workflow states --'
};

const statusNames: any = {
  [allMarker]: '-- All statuses --'
};

function WorkflowInstanceSearchForm(props: {
  definitions: Array<any>;
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
  const [businessKey, setBusinessKey] = useState<string>(
    queryParams.get('businessKey') || ''
  );
  const [externalId, setExternalId] = useState<string>(
    queryParams.get('externalId') || ''
  );
  const [id, setId] = useState<string>(queryParams.get('id') || '');
  const [parentInstanceId, setParentInstanceId] = useState<string>(
    queryParams.get('parentWorkflowId') || ''
  );

  const types = [allMarker].concat(props.definitions.map(d => d.type));

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
        businessKey,
        externalId,
        id,
        parentInstanceId
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
      for (let field of ['instanceId', 'parentInstanceId']) {
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
      history.push(`/search?${new URLSearchParams(data).toString()}`);
      props.onSubmit(data);
    },
    [
      businessKey,
      externalId,
      history,
      id,
      parentInstanceId,
      props,
      state,
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
  }, []);

  const setWorkflowType = (type: string) => {
    // when type is changed, need to reset all selections that depend on type
    setType(type);
    setState(allMarker);
  };

  return (
    <form className={classes.root}>
      <div>
        <Selection
          label="Workflow type"
          items={types}
          selected={type}
          onChange={setWorkflowType}
          getSelectionLabel={(type: string) => typeNames[type] || type}
        />

        <Selection
          label="Workflow state"
          items={states}
          selected={state}
          onChange={setState}
          getSelectionLabel={(state: string) => stateNames[state] || state}
        />

        <Selection
          label="Workflow status"
          items={statuses}
          selected={status}
          onChange={setStatus}
          getSelectionLabel={(status: string) => statusNames[status] || status}
        />
      </div>

      <div>
        <TextField
          label="Business key"
          value={businessKey}
          onChange={e => setBusinessKey(e.target.value)}
        />

        <TextField
          label="External id"
          value={externalId}
          onChange={e => setExternalId(e.target.value)}
        />

        <TextField
          label="Workflow id"
          type="number"
          value={id}
          onChange={e => setId(e.target.value)}
        />

        <TextField
          label="Workflow parent id"
          type="number"
          value={parentInstanceId}
          onChange={e => setParentInstanceId(e.target.value)}
        />
      </div>

      <Button onClick={handleSubmit} variant="contained">
        Search
      </Button>
    </form>
  );
}

export default WorkflowInstanceSearchForm;
