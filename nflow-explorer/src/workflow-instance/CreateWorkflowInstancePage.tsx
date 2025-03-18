import React, {useCallback, useEffect, useState} from 'react';
import {Typography, Grid, Container} from '@mui/material';
import {useLocation} from 'react-router-dom';

import {CreateWorkflowInstanceForm} from './CreateWorkflowInstanceForm';
import {Executor, WorkflowDefinition} from '../types';
import {useConfig} from '../config';
import {Spinner, StateGraph} from '../component';
import {listAllExecutors, listWorkflowDefinitions} from '../service';

export const SelectedDefinitionContext = React.createContext({
  selectedDefinition: undefined,
  setSelectedDefinition: (definition: WorkflowDefinition) => {}
});

const definitionFromType = (
  definitions: WorkflowDefinition[],
  type: string | null
) => definitions.filter(d => d.type === type)[0];

const ShowForm = ({
  definitions,
  executors
}: {
  definitions: WorkflowDefinition[];
  executors: Executor[];
}) => {
  const queryParams = new URLSearchParams(useLocation().search);
  const defaultDefinition =
    definitionFromType(definitions, queryParams.get('type')) || definitions[0];
  const [selectedDefinition, setSelectedDefinition] =
    useState<WorkflowDefinition>(defaultDefinition);

  return (
    <SelectedDefinitionContext.Provider
      value={{selectedDefinition, setSelectedDefinition} as any}
    >
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          {selectedDefinition && (
            <Container>
              <Typography variant="h4" gutterBottom>
                {selectedDefinition.type}
                &nbsp;&nbsp;&nbsp;
              </Typography>
              <Typography variant="h6" color="textSecondary" gutterBottom>
                {selectedDefinition.description}
              </Typography>
              <StateGraph definition={selectedDefinition} />
            </Container>
          )}
        </Grid>
        <Grid item xs={12} md={6}>
          <Container>
            <Typography variant="h4" gutterBottom>
              Create a new workflow instance
            </Typography>
            <CreateWorkflowInstanceForm
              definitions={definitions}
              executorGroups={executors}
            />
          </Container>
        </Grid>
      </Grid>
    </SelectedDefinitionContext.Provider>
  );
};

function CreateWorkflowInstancePage() {
  const config = useConfig();

  const [definitions, setDefinitions] = useState<WorkflowDefinition[]>();
  const [executors, setExecutors] = useState<Array<Executor>>([]);
  const [executorLoad, setExecutorLoad] = useState<boolean>(true);

  const fetchExecutors = useCallback(() => {
    listAllExecutors(config)
      .then(data => setExecutors(data))
      .catch(error => {
        // TODO error handling
        console.error('Error', error);
      })
      .finally(() => setExecutorLoad(false));
  }, [config]);

  useEffect(() => {
    listWorkflowDefinitions(config).then(setDefinitions);
    fetchExecutors();
  }, [config]);

  if (definitions && !executorLoad) {
    return <ShowForm definitions={definitions} executors={executors} />;
  }

  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Container>
          <Spinner />
        </Container>
      </Grid>
    </Grid>
  );
}

export {CreateWorkflowInstancePage};
