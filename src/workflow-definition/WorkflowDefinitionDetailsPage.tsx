import React, { useState, useEffect, useContext, Fragment } from "react";
import { Link, useParams } from "react-router-dom";

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
  }, [config, type]);

  const workflowDetails = (definition: WorkflowDefinition, statistics: WorkflowSummaryStatistics) => {
    return (
      <div>
        <h1>{definition.type}</h1>
        <blockquote>{definition.description}</blockquote>
        <p><Link to={searchPath}>Search related workflows</Link></p>
        <p>TODO put graph here</p>
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
    <span>
      Workflow definition {type} not found
    </span>
  );
}

export default WorkflowDefinitionDetailsPage;
