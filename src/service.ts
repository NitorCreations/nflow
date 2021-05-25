import {
  Config,
  Executor,
  WorkflowDefinition,
  WorkflowInstance,
  WorkflowStatistics,
  WorkflowSummaryStatistics,
} from "./types";
import { Cache } from "./cache";

const cacheWD = new Cache<Array<WorkflowDefinition>>(10 * 20 * 1000);

const convertDates = (dateFields: Array<string>) => (item: any) => {
  let newItem = { ...item };
  for (let field of dateFields) {
    if (item[field]) {
      item[field] = new Date(item[field]);
    }
  }
  return newItem;
};

const listExecutors = (config: Config): Promise<Array<Executor>> => {
  return fetch(config.baseUrl + "/api/v1/workflow-executor")
    .then((response) => response.json())
    .then((items: any) =>
      items.map(convertDates(["started", "stopped", "active", "expires"]))
    );
};

const listWorkflowDefinitions = (
  config: Config
): Promise<Array<WorkflowDefinition>> => {
  const url = config.baseUrl + "/api/v1/workflow-definition";
  const cached = cacheWD.get(url);
  if (cached) {
    console.info("Return from cache ", url);
    return Promise.resolve(cached);
  }
  console.info("Return from service ", url);
  return fetch(url)
    .then((response) => response.json())
    .then((response: Array<WorkflowDefinition>) =>
      cacheWD.setAndReturn(url, response)
    );
};

const getWorkflowDefinition = (config: Config, type: string): Promise<WorkflowDefinition> => {
  const url = config.baseUrl + "/api/v1/workflow-definition?type=" + type;
  return fetch(url)
    .then(response => response.json())
    // TODO how to handle Not found case?
    .then(response => response[0])
};

// if there are no instances in db => returns undefined?
const getWorkflowStatistics = (config: Config, type: string): Promise<WorkflowStatistics> => {
  const url = config.baseUrl + "/api/v1/statistics/workflow/" + type;
  return fetch(url)
    .then(response => response.json())
    .then(response => response.stateStatistics)
};

/**
 * Read structure that is used to show summary table in workflow definition page.
 */
const getWorkflowSummaryStatistics = (config: Config, type: string): Promise<WorkflowSummaryStatistics> => {
  return Promise.all([
    getWorkflowDefinition(config, type),
    getWorkflowStatistics(config, type) 
  ])
  .then(([def, stats]) => {
    // Gather all possible state names, from workflow definition and from the statistics.
    // It is possible that state name exists only in statistics (from an old version of the workflow definition).
    const definitionStates = def.states.map(state => state.id)
    for (const statKey of Object.keys(stats)) {
      for (const stateKey of Object.keys(stats[statKey])) {
        if (!definitionStates.includes(stateKey)) {
          definitionStates.push(stateKey);
        }
      }
    }
    // structure data suitable for summary table
    const result: any[] = [];
    const totalPerStatus: any = {};
    for (const state of definitionStates) {
      const row: any = {}
      let totalPerState = 0;
      for (const status of ['created', 'inProgress', 'executing', 'manual', 'finished']) {
        const queuingPossible = status === 'created' || status === 'inProgress';
        const itemNumber: any = (stats[state] as any || {})[status] || 
          (queuingPossible ? {allInstances: 0, queuedInstances: 0} : {allInstances: 0});
        row[status] = itemNumber
        totalPerState += itemNumber.allInstances;
        totalPerStatus[status] = (totalPerStatus[status] || {allInstances: 0}) ;
        totalPerStatus[status].allInstances += itemNumber.allInstances;
        if (queuingPossible) {
          totalPerStatus[status].queuedInstances = (totalPerStatus[status].queuedInstances || 0);
          totalPerStatus[status].queuedInstances += itemNumber.queuedInstances;
        }
      }
      result.push({state, stats: row, total: totalPerState});
    }
    return {stats: result, totalPerStatus};
  });
};

const listWorkflowInstances = (
  config: Config,
  query?: any
): Promise<Array<WorkflowInstance>> => {
  const params = new URLSearchParams(query).toString();
  return fetch(
    config.baseUrl + "/api/v1/workflow-instance?" + params.toString()
  )
    .then((response) => response.json())
    .then((items: any) =>
      // TODO actions have fields executionStart, executionEnd
      items.map(
        convertDates(["nextActivation", "created", "modified", "started"])
      )
    );
};


const getWorkflowInstance = (config: Config, id: number): Promise<WorkflowInstance> => {
  const url = config.baseUrl + "/api/v1/workflow-instance/id/" + id + "?include=actions,currentStateVariables,actionStateVariables";
  return fetch(url)
    .then(response => response.json())
    .then(convertDates(["nextActivation", "created", "modified", "started"]))
    // TODO how to handle Not found case?
};


export { 
  listExecutors, 
  listWorkflowDefinitions, getWorkflowDefinition, 
  getWorkflowStatistics, getWorkflowSummaryStatistics,
  listWorkflowInstances, getWorkflowInstance,
};
