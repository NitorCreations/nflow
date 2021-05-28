import React, { useState, useEffect, useContext } from "react";
import { useParams } from "react-router-dom";
import { Typography, Grid, Container, Paper } from '@material-ui/core';

import { StateGraph, InternalLink, Spinner } from "../component";
import { ConfigContext } from "../config";
import { getWorkflowDefinition, getWorkflowSummaryStatistics,  } from "../service";
import { WorkflowDefinition, WorkflowSummaryStatistics } from "../types";
import { StatisticsSummaryTable } from "./StatisticsSummaryTable";
import { SettingsTable } from "./SettingsTable";

function WorkflowDefinitionDetailsPage() {
  let { type } = useParams() as any;
  const config = useContext(ConfigContext);
  const [loading, setLoading] = useState<boolean>(true)

  const [definition, setDefinition] = useState<WorkflowDefinition>()
  const [statistics, setStatistics] = useState<WorkflowSummaryStatistics>()

  // TODO new features
  // launch a new instance

  // TODO skipped features
  // radiator

  useEffect(() => {
    setLoading(true);
    Promise.all([
      getWorkflowDefinition(config, type),
      getWorkflowSummaryStatistics(config, type)
    ]).then(([def, stats]) => {
      setDefinition(def);
      setStatistics(stats);
    }).catch((e) => {
      // TODO handler error
      console.error(e);
    }).finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [config, type]);

  const workflowDetails = (definition: WorkflowDefinition, statistics: WorkflowSummaryStatistics) => {
    const searchPath = "/search?type=" + type;
    const createPath = "/workflow/create?type=" + type;
    return (

      <Grid container spacing={3}>
        <Grid item xs={12} sm={6}>
          <Container>
          <Typography variant="h2">{definition.type}</Typography>
          <blockquote>{definition.description}</blockquote>
          <div><InternalLink to={searchPath}>Search related workflows</InternalLink></div>
          <div><InternalLink to={createPath}>Create a new workflow instance</InternalLink></div>
          <StateGraph definition={definition} />
          </Container>
        </Grid>
        <Grid item xs={12} sm={6}>
          <Container>
            <Paper>
              <Typography variant="h3">Settings</Typography>
              <SettingsTable definition={definition} />
            </Paper>
            <Paper>
              <Typography variant="h3">Statistics summary</Typography>
              <StatisticsSummaryTable statistics={statistics} />
            </Paper>
          </Container>
        </Grid>
      </Grid>
    );
  };

  if (definition && statistics) {
    return workflowDetails(definition, statistics);
  }
  if (loading) {
    return (
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Spinner />
        </Grid>
      </Grid>
    )
  }
  return (
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Typography>
          Workflow definition {type} not found
        </Typography>
      </Grid>
    </Grid>
  );
}

export default WorkflowDefinitionDetailsPage;
