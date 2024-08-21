import React, {useContext, useState} from 'react';
import {debounce} from 'lodash';
import {useLocation, useHistory} from 'react-router-dom';
import {Container, Button, TextField} from '@material-ui/core';
import {makeStyles} from '@material-ui/core/styles';
import {Selection, useFeedback} from '../component';
import {
  Executor,
  NewWorkflowInstance,
  NewWorkflowInstanceResponse,
  WorkflowDefinition
} from '../types';
import {createWorkflowInstance} from '../service';
import {SelectedDefinitionContext} from './CreateWorkflowInstancePage';
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
  executorGroups: Array<any>;
}) {
  const feedback = useFeedback();
  const config = useContext(ConfigContext);
  const history = useHistory();
  const classes = useStyles();
  const selectedDefinitionContext = useContext(SelectedDefinitionContext);
  const queryParams = new URLSearchParams(useLocation().search);
  const definitionFromType = (type: string | null) =>
    props.definitions.filter(d => d.type === type)[0];

  const [externalId, setExternalId] = useState<string>(
    queryParams.get('externalId') || ''
  );
  const [businessKey, setBusinessKey] = useState<string>(
    queryParams.get('businessKey') || ''
  );
  const [executorGroup, setExecutorGroup] = useState<string>(
    queryParams.get('executorGroups') || ''
  );
  const [stateVariables, setStateVariables] = useState<string>(
    queryParams.get('stateVariables') || ''
  );
  const [stateVariablesParsed, setStateVariablesParsed] = useState<{
    [key: string]: any;
  }>();

  const [stateVariableError, setStateVariableError] = useState<
    string | undefined
  >();

  const executorGroups = props.executorGroups
    .map((executorGroup: Executor) => executorGroup.executorGroup)
    //remove duplicates
    .filter((value, index, self) => self.indexOf(value) === index);

  const selectDefinition = (type: string) => {
    const definition = definitionFromType(type);
    selectedDefinitionContext.setSelectedDefinition(definition);
  };

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
    } catch (err: any) {
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
    return !stateVariableError && executorGroup.trim() !== '';
  };

  // TODO belongs to Page class?
  const sendCreateRequest = () => {
    // TODO activationTime
    // TODO startState
    const data: NewWorkflowInstance = {
      type: (selectedDefinitionContext.selectedDefinition! as any).type,
      executorGroup: executorGroup,
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
      <Container className="create-workflow-container">
        <Selection
          label="Workflow definition"
          items={definitionNames}
          selected={(selectedDefinitionContext.selectedDefinition! as any).type}
          onChange={selectDefinition}
          getSelectionLabel={(x: any) => x}
        />
        <Selection
          label="Executor Group"
          items={executorGroups}
          selected={executorGroup}
          onChange={setExecutorGroup}
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
        <div>
          <TextField
            fullWidth
            error={!!stateVariableError}
            helperText={stateVariableError}
            className="json-field"
            label="State variables"
            InputLabelProps={{shrink: true}}
            placeholder="Add state variables as a JSON document"
            multiline
            minRows={10}
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
    </form>
  );
}

export {CreateWorkflowInstanceForm};
