import React, { useState, useEffect, useContext } from "react";
import { Link, useParams } from "react-router-dom";
import Typography from '@material-ui/core/Typography';

import { Spinner } from "../component";
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

  // TODO required features
  // state graph

  // TODO new features
  // launch a new instance

  // TODO skipped features
  // radiator
  const searchPath = "/search?type=" + type;

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
    return (
      <div>
        <Typography variant="h2">{definition.type}</Typography>
        <blockquote>{definition.description}</blockquote>
        <Typography><Link to={searchPath}>Search related workflows</Link></Typography>
        <Typography>TODO put graph here</Typography>
        <SettingsTable definition={definition} />
        <StatisticsSummaryTable statistics={statistics} />
      </div>
    );
  };

  if (definition && statistics) {
    return workflowDetails(definition, statistics);
  }
  if (loading) {
    return (
      <Spinner />
    )
  }
  return (
    <Typography>
      Workflow definition {type} not found
    </Typography>
  );
}

export default WorkflowDefinitionDetailsPage;
