import {
  Config,
  Executor,
  WorkflowDefinition,
  WorkflowInstance,
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

export { listExecutors, listWorkflowDefinitions, listWorkflowInstances };
