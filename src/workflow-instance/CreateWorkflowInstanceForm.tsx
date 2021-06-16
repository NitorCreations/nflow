import React, {useContext, useState} from 'react';
import {debounce} from 'lodash';
import {useLocation, useHistory} from 'react-router-dom';
import {
  Container,
  Button,
  TextField,
  Paper,
  Typography
} from '@material-ui/core';
import {makeStyles} from '@material-ui/core/styles';
import {Selection, StateGraph, useFeedback} from '../component';
import {
  NewWorkflowInstance,
  NewWorkflowInstanceResponse,
  WorkflowDefinition
} from '../types';
import {createWorkflowInstance} from '../service';
import {ConfigContext} from '../config';
import './workflow-instance.scss';

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

function CreateWorkflowInstanceForm(props: {
  definitions: WorkflowDefinition[];
}) {
  const feedback = useFeedback();
  const config = useContext(ConfigContext);
  const history = useHistory();
  const classes = useStyles();
  const queryParams = new URLSearchParams(useLocation().search);
  const definitionFromType = (type: string | null) =>
    props.definitions.filter(d => d.type === type)[0];

  const defaultDefinition = () =>
    definitionFromType(queryParams.get('type')) || props.definitions[0];
  const [definition, setDefinition] = useState<WorkflowDefinition>(
    defaultDefinition()
  );
  const [externalId, setExternalId] = useState<string>(
    queryParams.get('externalId') || ''
  );
  const [businessKey, setBusinessKey] = useState<string>(
    queryParams.get('businessKey') || ''
  );
  const [stateVariables, setStateVariables] = useState<string>(
    queryParams.get('stateVariables') || ''
  );
  const [stateVariablesParsed, setStateVariablesParsed] =
    useState<{[key: string]: any}>();

  const [stateVariableError, setStateVariableError] =
    useState<string | undefined>();

  const selectDefinition = (type: string) =>
    setDefinition(definitionFromType(type));

  // Do not run validation every time a new character is added (debounce).
  const validateJSON = debounce((value: string) => {
    if (value.trim() === '') {
      setStateVariableError(undefined);
      return;
    }
    try {
      const object = JSON.parse(value);
      setStateVariableError(undefined);
      // TODO check that the object is an object? Lists are not allowed here?
      setStateVariablesParsed(object);
    } catch (err) {
      setStateVariableError(err.message);
      setStateVariablesParsed(undefined);
    }
  }, 500);

  const setStateVariablesStr = (value: string) => {
    setStateVariables(value);
    // TODO it is possible to change json to invalid, then submit before validation kicks in
    validateJSON(value);
  };

  const definitionNames = props.definitions.map(definition => definition.type);

  const formValid = () => {
    return !stateVariableError;
  };

  // TODO belongs to Page class?
  const sendCreateRequest = () => {
    // TODO activationTime
    // TODO startState
    const data: NewWorkflowInstance = {
      type: definition.type,
      businessKey: businessKey || undefined,
      externalId: externalId || undefined,
      activationTime: undefined,
      // if activate=false, then nextActivation=null means null
      // if activate=true, then nextActivation=null means now
      activate: true,
      stateVariables: stateVariablesParsed
    };
    createWorkflowInstance(config, data)
      .then((response: NewWorkflowInstanceResponse) => {
        console.info(`A new workflow was created. id=${response.id}`, response);
        feedback.addFeedback({
          message: `A new workflow was created`,
          severity: 'success'
        });
        history.push('/workflow/' + response.id);
      })
      .catch(err => {
        console.error('Creating workflow failed', err);
        feedback.addFeedback({
          message: 'Creating workflow failed: ' + err.message,
          severity: 'error'
        });
      });
  };

  return (
    <form className={classes.root}>
      <Paper>
        <Container className="create-workflow-container">
          <Selection
            label="Workflow definition"
            items={definitionNames}
            selected={definition.type}
            onChange={selectDefinition}
            getSelectionLabel={(x: any) => x}
          />
          <TextField
            label="External id"
            value={externalId}
            onChange={(e: any) => setExternalId(e.target.value)}
          />
          <TextField
            label="Business key"
            value={businessKey}
            onChange={(e: any) => setBusinessKey(e.target.value)}
          />
          {/* TODO need to set bigger width for the field */}
          <div>
            <TextField
              error={!!stateVariableError}
              helperText={stateVariableError}
              className="json-field"
              label="State variables"
              InputLabelProps={{shrink: true}}
              placeholder="Add state variables as a JSON document"
              multiline
              rows={10}
              value={stateVariables}
              onChange={(e: any) => setStateVariablesStr(e.target.value)}
            />
          </div>
          <div>
            <Button
              variant="contained"
              onClick={sendCreateRequest}
              disabled={!formValid()}
            >
              Create
            </Button>
          </div>
        </Container>
      </Paper>
      <Paper>
        <Container>
          <Typography variant="h3">{definition.type}</Typography>
          <Typography>{definition.description}</Typography>
          <StateGraph definition={definition} />
        </Container>
      </Paper>
    </form>
  );
}

export {CreateWorkflowInstanceForm};
