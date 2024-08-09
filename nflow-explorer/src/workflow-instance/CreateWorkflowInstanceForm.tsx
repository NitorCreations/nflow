import React, {useContext, useState} from 'react';
import {debounce} from 'lodash';
import {useLocation, useNavigate} from 'react-router-dom';
import {Container, Button, TextField, createTheme, Grid} from '@mui/material';
import {Selection, useFeedback} from '../component';
import {
  NewWorkflowInstance,
  NewWorkflowInstanceResponse,
  WorkflowDefinition
} from '../types';
import {createWorkflowInstance} from '../service';
import {SelectedDefinitionContext} from './CreateWorkflowInstancePage';
import {ConfigContext} from '../config';
import './workflow-instance.scss';
import {ThemeProvider} from "@mui/material/styles";

const customMuiTheme = createTheme({
  components: {
    MuiFormControl: {
      styleOverrides: {
        root: {
          //margin: theme.spacing(1),
          minWidth: 200,
          maxWidth: 500
        }
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          margin: "1rem"
        }
      }
    }
  }
});

function CreateWorkflowInstanceForm(props: {
  definitions: WorkflowDefinition[];
}) {
  const feedback = useFeedback();
  const config = useContext(ConfigContext);
  const navigate = useNavigate();
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
  const [stateVariables, setStateVariables] = useState<string>(
    queryParams.get('stateVariables') || ''
  );
  const [stateVariablesParsed, setStateVariablesParsed] = useState<{
    [key: string]: any;
  }>();

  const [stateVariableError, setStateVariableError] = useState<
    string | undefined
  >();

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
    return !stateVariableError;
  };

  // TODO belongs to Page class?
  const sendCreateRequest = () => {
    // TODO activationTime
    // TODO startState
    const data: NewWorkflowInstance = {
      type: (selectedDefinitionContext.selectedDefinition! as any).type,
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
        navigate('/workflow/' + response.id);
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
    <ThemeProvider theme={customMuiTheme}>
      <form>
        <Grid container>
          <Grid item xs={4}>
            <Selection
              label="Workflow definition"
              items={definitionNames}
              selected={(selectedDefinitionContext.selectedDefinition! as any).type}
              onChange={selectDefinition}
              getSelectionLabel={(x: any) => x}
            />
          </Grid>
          <Grid item xs={4}>
            <TextField
              label="External id"
              value={externalId}
              onChange={(e: any) => setExternalId(e.target.value)}
              variant="standard"
              sx={{margin: "0rem"}}
            />
          </Grid>
          <Grid item xs={4}>
            <TextField
              label="Business key"
              value={businessKey}
              onChange={(e: any) => setBusinessKey(e.target.value)}
              variant="standard"
              sx={{margin: "0rem"}}
            />
          </Grid>
          <Grid item xs={12}>
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
              variant="standard"
            />
          </Grid>
          <Grid item xs={2}>
            <Button
              variant="contained"
              onClick={sendCreateRequest}
              disabled={!formValid()}
            >
              Create
            </Button>
          </Grid>
        </Grid>
      </form>
    </ThemeProvider>
  );
}

export {CreateWorkflowInstanceForm};
