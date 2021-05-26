import React, { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import TextField from '@material-ui/core/TextField';
import Button from '@material-ui/core/Button';
import Select from '@material-ui/core/Select';
import FormControl from '@material-ui/core/FormControl';
import InputLabel from '@material-ui/core/InputLabel';
import MenuItem from '@material-ui/core/MenuItem';
import { makeStyles, useTheme } from '@material-ui/core/styles';

const useStyles = makeStyles((theme) => ({
  formControl: {
    margin: theme.spacing(1),
    minWidth: 200,
    maxWidth: 500,
  },
  root: {
    '& .MuiTextField-root': {
      margin: theme.spacing(1),
    },
  },
}));

const allMarker = '!-all-!';

const statuses = [
  allMarker,
  "created",
  "inProgress",
  "finished",
  "manual",
  "executing",
];

const typeNames: any = {
  [allMarker]: "-- All workflow types --",
};

const stateNames: any = {
  [allMarker]: "-- All workflow states --",
};

const statusNames: any = {
  [allMarker]: "-- All statuses --",
};

function WorkflowInstanceSearchForm(props: {
  definitions: Array<any>;
  onSubmit: (data: any) => any;
}) {
  const theme = useTheme();
  const classes = useStyles();

  const queryParams = new URLSearchParams(useLocation().search);
  
  const [type, setType] = useState<string>(queryParams.get("type") || allMarker);
  const [state, setState] = useState<string>(queryParams.get("state") || allMarker);
  const [status, setStatus] = useState<string>(queryParams.get("status") || allMarker);
  const [businessKey, setBusinessKey] = useState<string>(
    queryParams.get("businessKey") || ""
  );
  const [externalId, setExternalId] = useState<string>(
    queryParams.get("externalId") || ""
  );
  const [id, setId] = useState<string>(queryParams.get("id") || "");
  const [parentInstanceId, setParentInstanceId] = useState<string>(
    queryParams.get("parentWorkflowId") || ""
  );

  const types = [allMarker].concat(props.definitions.map((d) => d.type));

  // TODO to lodash or not to lodash?
  const selectedWorkflow = props.definitions.filter((d) => d.type === type)[0];
  const selectedStates = (
    (selectedWorkflow && selectedWorkflow.states) ||
    []
  ).map((s: any) => s.id);
  const states = [allMarker].concat(selectedStates);

  const handleSubmit = (e?: any) => {
    e && e.preventDefault();
    let data: any = {
      type,
      state,
      status,
      businessKey,
      externalId,
      id,
      parentInstanceId,
    };
    for (let field of Object.keys(data)) {
      if (data[field]) {
        data[field] = data[field].trim();
      }
      if (data[field] === allMarker) {
        data[field] = undefined
      }
    }
    // remove empty fields, convert ids to numbers
    for (let field of ["instanceId", "parentInstanceId"]) {
      if (field in data) {
        data[field] = parseInt(data[field], 10) || undefined;
      }
    }
    for (let k of Object.keys(data)) {
      if (data[k] === "" || Number.isNaN(data[k]) || data[k] === undefined) {
        delete data[k];
      }
    }
    props.onSubmit(data);
  };

  // if query parameters are give, trigger immediate search
  useEffect(() => {
    if (queryParams.keys().next()?.value) {
      handleSubmit();
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const setWorkflowType = (type: string) => {
    // when type is changed, need to reset all selections that depend on type
    setType(type);
    setState(allMarker);
  };

  // TODO move to components
  const selection = (
    label: string,
    items: Array<any>,
    selected: string,
    onChange: (v: string) => any,
    nameMap: any
  ) => {
    return (
      <FormControl className={classes.formControl}>
      <InputLabel id="demo-simple-select-label">{label}</InputLabel>
      <Select
        labelId="demo-simple-select-label"
        id="demo-simple-select"
        value={selected}
        onChange={(e: any) => onChange(e.target.value)}
      >
        {items.map((item) => (
          <MenuItem key={item} value={item}>
            {nameMap[item] || item}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
    );
  };

  return (
    <form className={classes.root}>
      <div>
          {selection('Workflow type', types, type, setWorkflowType, typeNames)}
  
          {selection('Workflow state', states, state, setState, stateNames)}

          {selection('Workflow status', statuses, status, setStatus, statusNames)}
      </div>

      <div>
        <TextField
          label="Business key"
          value={businessKey}
          onChange={(e) => setBusinessKey(e.target.value)}
        />

        <TextField
          label="External id"
          value={externalId}
          onChange={(e) => setExternalId(e.target.value)}
        />

        <TextField
          label="Workflow id"
          type="number"
          value={id}
          onChange={(e) => setId(e.target.value)}
        />

      <TextField
          label="Workflow parent id"
          type="number"
          value={parentInstanceId}
          onChange={(e) => setParentInstanceId(e.target.value)}
        />
      </div>

      <Button onClick={handleSubmit} variant="contained">Search</Button>
    </form>
  );
}

export default WorkflowInstanceSearchForm;
