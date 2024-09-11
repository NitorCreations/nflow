import React, {useEffect, useState} from 'react';
import {Typography, Grid, Container} from '@mui/material';
import {useLocation} from 'react-router-dom';

import {CreateWorkflowInstanceForm} from './CreateWorkflowInstanceForm';
import {WorkflowDefinition} from '../types';
import {useConfig} from '../config';
import {Spinner, StateGraph} from '../component';
import {listWorkflowDefinitions} from '../service';

export const SelectedDefinitionContext = React.createContext({
  selectedDefinition: undefined,
  setSelectedDefinition: (definition: WorkflowDefinition) => {}
});

const definitionFromType = (
  definitions: WorkflowDefinition[],
  type: string | null
) => definitions.filter(d => d.type === type)[0];

const ShowForm = ({definitions}: {definitions: WorkflowDefinition[]}) => {
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
            <CreateWorkflowInstanceForm definitions={definitions} />
          </Container>
        </Grid>
      </Grid>
    </SelectedDefinitionContext.Provider>
  );
};

function CreateWorkflowInstancePage() {
  const config = useConfig();

  const [definitions, setDefinitions] = useState<WorkflowDefinition[]>();

  useEffect(() => {
    listWorkflowDefinitions(config).then(setDefinitions);
  }, [config]);

  if (definitions) {
    return <ShowForm definitions={definitions} />;
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
