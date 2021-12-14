import React, {useState, useEffect} from 'react';
import {useParams} from 'react-router-dom';
import {Typography, Grid, Container, Paper} from '@material-ui/core';
import AddCircleOutlineOutlinedIcon from '@material-ui/icons/AddCircleOutlineOutlined';
import Search from '@material-ui/icons/Search';

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
  type,
  externalContent
}: {
  definition: WorkflowDefinition;
  statistics: WorkflowSummaryStatistics;
  type: string;
  externalContent: any;
}) => {
  const searchPath = `/workflow?type=${type}`;
  const createPath = `/workflow/create?type=${type}`;

  return (
    <Grid container spacing={3}>
      <Grid item xs={12} sm={6}>
        <Container>
          <Typography variant="h4" gutterBottom>
            {definition.type}
            &nbsp;&nbsp;&nbsp;
            <InternalLink to={createPath}>
              <AddCircleOutlineOutlinedIcon fontSize="inherit" />
            </InternalLink>
            &nbsp;&nbsp;
            <InternalLink to={searchPath}>
              <Search fontSize="inherit" />
            </InternalLink>
          </Typography>
          <Typography variant="h6" color="textSecondary" gutterBottom>
            {definition.description}
          </Typography>
          <div dangerouslySetInnerHTML={{__html: externalContent}} />
          <StateGraph definition={definition} />
        </Container>
      </Grid>
      <Grid item xs={12} sm={6}>
        <Container>
          <Paper className="workflow-definition-paper">
            <Typography variant="h5" gutterBottom>
              Statistics summary
            </Typography>
            <StatisticsSummaryTable statistics={statistics} />
          </Paper>
          <Paper className="workflow-definition-paper">
            <Typography variant="h5" gutterBottom>
              Settings
            </Typography>
            <SettingsTable definition={definition} />
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
  const [externalContent, setExternalContent] = useState<any>();

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
        return def;
      })
      .then(definition => {
        Promise.resolve(
          config.customDefinitionContent &&
            config.customDefinitionContent(definition)
        ).then(content => {
          setExternalContent(content);
        });
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
        externalContent={externalContent}
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
