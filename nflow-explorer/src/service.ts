import {
  Config,
  Executor,
  WorkflowDefinition,
  WorkflowInstance,
  WorkflowStatistics,
  WorkflowSummaryStatistics,
  NewWorkflowInstance,
  NewWorkflowInstanceResponse
} from './types';
import {Cache} from './cache';

const cacheWD = new Cache<Array<WorkflowDefinition>>(10 * 20 * 1000);

// TODO timeouts to requests?

const serviceUrl = (config: Config, servicePath: string) => {
  return config.activeNflowEndpoint.apiUrl + servicePath;
};

const convertDates = (dateFields: Array<string>) => (item: any) => {
  let newItem = {...item};
  for (let field of dateFields) {
    if (newItem[field]) {
      newItem[field] = new Date(item[field]);
    }
  }
  return newItem;
};

const convertExecutor = (executor: any) => {
  return convertDates(['started', 'stopped', 'active', 'expires'])(executor);
};

const convertWorkflowInstance = (instance: any) => {
  // TODO actions have fields executionStart, executionEnd
  return convertDates(['nextActivation', 'created', 'modified', 'started'])(
    instance
  );
};

const authenticatedApiCall = (url: string, config: Config, body?: any): Promise<any> => {
  const options: RequestInit = {
    method: body ? "PUT" : "GET",
    body: body
  };
  if (!config.msalClient) {
    options.headers = new Headers({
      "content-type": "application/json"
    });
    return fetch(url, options);
  }
  const request = {
    scopes: ["openid"],
  };
  config.msalClient.setActiveAccount(config.msalClient.getAllAccounts()[0])  // required by acquireTokenSilent
  return config.msalClient.acquireTokenSilent(request)
    .then(tokenResponse => {
      options["headers"] = new Headers({
        "Authorization": "Bearer " + tokenResponse.accessToken,
        "content-type": "application/json"
      });
      return fetch(url, options)
    });
}

const listExecutors = (config: Config): Promise<Array<Executor>> => {
  return authenticatedApiCall(serviceUrl(config, '/v1/workflow-executor'), config)
    .then(response => response.json())
    .then((items: any) => items.map(convertExecutor));
};

const listWorkflowDefinitions = (
  config: Config
): Promise<Array<WorkflowDefinition>> => {
  const url = serviceUrl(config, '/v1/workflow-definition');
  const cached = cacheWD.get(url);
  if (cached) {
    return Promise.resolve(cached);
  }
  return authenticatedApiCall(url, config)
    .then(response => response.json())
    .then((response: Array<WorkflowDefinition>) =>
      cacheWD.setAndReturn(url, response)
    );
};

const getWorkflowDefinition = (
  config: Config,
  type: string
): Promise<WorkflowDefinition> => {
  const url = serviceUrl(config, '/v1/workflow-definition?type=' + type);
  return (
    authenticatedApiCall(url, config)
      .then(response => response.json())
      // TODO how to handle Not found case?
      .then(response => response[0])
  );
};

// if there are no instances in db => returns undefined?
const getWorkflowStatistics = (
  config: Config,
  type: string
): Promise<WorkflowStatistics> => {
  const url = serviceUrl(config, '/v1/statistics/workflow/' + type);
  return authenticatedApiCall(url, config)
    .then(response => response.json())
    .then(response => response.stateStatistics);
};

/**
 * Read structure that is used to show summary table in workflow definition page.
 */
const getWorkflowSummaryStatistics = (
  config: Config,
  type: string
): Promise<WorkflowSummaryStatistics> => {
  return Promise.all([
    getWorkflowDefinition(config, type),
    getWorkflowStatistics(config, type)
  ]).then(([def, stats]) => {
    stats = stats || {};
    // Gather all possible state names, from workflow definition and from the statistics.
    // It is possible that state name exists only in statistics (from an old version of the workflow definition).
    const definitionStates = def.states.map(state => state.id);
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
      const row: any = {};
      let totalPerState = 0;
      for (const status of [
        'created',
        'inProgress',
        'executing',
        'manual',
        'finished'
      ]) {
        const queuingPossible = status === 'created' || status === 'inProgress';
        const itemNumber: any =
          ((stats[state] as any) || {})[status] ||
          (queuingPossible
            ? {allInstances: 0, queuedInstances: 0}
            : {allInstances: 0});
        row[status] = itemNumber;
        totalPerState += itemNumber.allInstances;
        totalPerStatus[status] = totalPerStatus[status] || {allInstances: 0};
        totalPerStatus[status].allInstances += itemNumber.allInstances;
        if (queuingPossible) {
          totalPerStatus[status].queuedInstances =
            totalPerStatus[status].queuedInstances || 0;
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
): Promise<WorkflowInstance[]> => {
  const params = new URLSearchParams(query).toString();
  return authenticatedApiCall(serviceUrl(config, '/v1/workflow-instance?' + params.toString()), config)
    .then(response => response.json())
    .then((items: any) => items.map(convertWorkflowInstance));
};

const listChildWorkflowInstances = (
  config: Config,
  id: number
): Promise<WorkflowInstance[]> => {
  const url = serviceUrl(
    config,
    '/v1/workflow-instance?parentWorkflowId=' + id
  );
  return authenticatedApiCall(url, config)
    .then(response => response.json())
    .then((items: any) => items.map(convertWorkflowInstance));
};

const getWorkflowInstance = (
  config: Config,
  id: number
): Promise<WorkflowInstance> => {
  const url = serviceUrl(
    config,
    '/v1/workflow-instance/id/' +
      id +
      '?include=actions,currentStateVariables,actionStateVariables'
  );
  return authenticatedApiCall(url, config)
    .then(response => response.json())
    .then(convertWorkflowInstance);
  // TODO how to handle Not found case?
};

const createWorkflowInstance = (
  config: Config,
  data: NewWorkflowInstance
): Promise<NewWorkflowInstanceResponse> => {
  const url = serviceUrl(config, '/v1/workflow-instance');
  return authenticatedApiCall(url, config, JSON.stringify(data))
    .then(response => response.json());
};

const updateWorkflowInstance = (
  config: Config,
  workflowId: number,
  data: any
): Promise<any> => {
  const url = serviceUrl(config, '/v1/workflow-instance/id/' + workflowId);
  return authenticatedApiCall(url, config, JSON.stringify(data));
};

const sendWorkflowInstanceSignal = (
  config: Config,
  workflowId: number,
  data: any
): Promise<any> => {
  const url = serviceUrl(
    config,
    '/v1/workflow-instance/' + workflowId + '/signal'
  );
  return authenticatedApiCall(url, config, JSON.stringify(data));
};

export {
  listExecutors,
  listWorkflowDefinitions,
  getWorkflowDefinition,
  getWorkflowStatistics,
  getWorkflowSummaryStatistics,
  listWorkflowInstances,
  getWorkflowInstance,
  listChildWorkflowInstances,
  createWorkflowInstance,
  sendWorkflowInstanceSignal,
  updateWorkflowInstance
};
