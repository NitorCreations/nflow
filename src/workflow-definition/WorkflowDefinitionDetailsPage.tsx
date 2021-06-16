import React, {useState, useEffect} from 'react';
import {useParams} from 'react-router-dom';
import {Typography, Grid, Container, Paper} from '@material-ui/core';

import {StateGraph, InternalLink, Spinner} from '../component';
import {useConfig} from '../config';
import {getWorkflowDefinition, getWorkflowSummaryStatistics} from '../service';
import {WorkflowDefinition, WorkflowSummaryStatistics} from '../types';
import {StatisticsSummaryTable} from './StatisticsSummaryTable';
import {SettingsTable} from './SettingsTable';
import './workflow-definition.scss';

const WorkflowDetails = ({
  definition,
  statistics,
  type
}: {
  definition: WorkflowDefinition;
  statistics: WorkflowSummaryStatistics;
  type: string;
}) => {
  const searchPath = `/search?type=${type}`;
  const createPath = `/workflow/create?type=${type}`;

  return (
    <Grid container spacing={3}>
      <Grid item xs={12} sm={6}>
        <Container>
          <Typography variant="h2" gutterBottom>
            {definition.type}
          </Typography>
          <blockquote>{definition.description}</blockquote>
          <div>
            <InternalLink to={searchPath}>
              Search related workflows
            </InternalLink>
          </div>
          <div>
            <InternalLink to={createPath}>
              Create a new workflow instance
            </InternalLink>
          </div>
          <StateGraph definition={definition} />
        </Container>
      </Grid>
      <Grid item xs={12} sm={6}>
        <Container>
          <Paper className="workflow-definition-paper">
            <Typography variant="h3" gutterBottom>
              Settings
            </Typography>
            <SettingsTable definition={definition} />
          </Paper>
          <Paper className="workflow-definition-paper">
            <Typography variant="h3" gutterBottom>
              Statistics summary
            </Typography>
            <StatisticsSummaryTable statistics={statistics} />
          </Paper>
        </Container>
      </Grid>
    </Grid>
  );
};

function WorkflowDefinitionDetailsPage() {
  const {type} = useParams() as any;
  const config = useConfig();
  const [loading, setLoading] = useState<boolean>(true);

  const [definition, setDefinition] = useState<WorkflowDefinition>();
  const [statistics, setStatistics] = useState<WorkflowSummaryStatistics>();

  // TODO new features
  // launch a new instance

  // TODO skipped features
  // radiator

  useEffect(() => {
    setLoading(true);
    Promise.all([
      getWorkflowDefinition(config, type),
      getWorkflowSummaryStatistics(config, type)
    ])
      .then(([def, stats]) => {
        setDefinition(def);
        setStatistics(stats);
      })
      .catch(e => {
        // TODO handler error
        console.error(e);
      })
      .finally(() => setLoading(false));
  }, [config, type]);

  if (definition && statistics) {
    return (
      <WorkflowDetails
        type={type}
        definition={definition}
        statistics={statistics}
      />
    );
  }

  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        {loading ? (
          <Spinner />
        ) : (
          <Typography>Workflow definition {type} not found</Typography>
        )}
      </Grid>
    </Grid>
  );
}

export default WorkflowDefinitionDetailsPage;
