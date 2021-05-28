interface Config {
  baseUrl: string;
  refreshSeconds: number;
}

interface Executor {
  id: number;
  host: string;
  pid: number;
  executorGroup: string;
  started: Date;
  stopped?: Date;
  active?: Date;
  expires?: Date;
}

interface WorkflowState {
  id: string;
  type: "start" | "normal" | "wait" | "manual" | "end";
  description?: string;
  transitions: Array<string>;
  onFailure?: string;
}

interface WorkflowDefinition {
  type: string;
  description?: string;
  onError: string;
  states: Array<WorkflowState>;

  settings: any;
}

interface WorkflowInstanceAction {
  id: number;
  workflowInstanceId: number;
  executorId: number;
  type:
    | "stateExecution"
    | "stateExecutionFailed"
    | "externalChange"
    | "recovery";
  state: string;
  stateText?: string;
  updatedStateVariables: { [key: string]: string };
  retryNo: number;
  executionStartTime: Date;
  executionEndTime: Date;
}

interface WorkflowInstance {
  id: number;
  type: string;
  status: "created" | "inProgress" | "executing" | "manual" | "finished";
  state: string;
  stateText?: string;
  nextActivation?: Date;
  stateVariables: { [key: string]: string };
  originalVariables: { [key: string]: string };
  actions: Array<WorkflowInstanceAction>;
  retries: number;
  created: Date;
  modified: Date;
  started: Date;
  executorGroup: string;
  childWorkflows: any;
  parentWorkflowId?: number;
  parentActionId?: number;
  priority?: number;
  businessKey?: string;
  externalId: string;
};

interface StateStatistics {
  created: {allInstances: number, queuedInstances: 0};
  inProgress: {allInstances: number, queuedInstances: 0};
  executing: {allInstances: number};
  manual: {allInstances: number};
  finished: {allInstances: number};
};
interface WorkflowStatistics {
  [key: string]: StateStatistics
};

interface WorkflowSummaryStatistics {
  stats: Array<
    {
      state: string,
      stats: {[status: string]: {allInstances: number, queuedInstances?: 0}},
      total: number,
    }
  >
  totalPerStatus: {[status: string]: {allInstances: number, queuedInstances?: 0}}
}

/**
 * Used when creating a new Workflow instance to database
 */
interface NewWorkflowInstance {
  type: string;
  businessKey?: string;
  externalId?: string;
  activationTime?: Date;
  activate: boolean;
  stateVariables?: {[key: string]: any};
}

interface NewWorkflowInstanceResponse {
  id: number;
  type: string
  externalId: string;
}

type FeedbackMessage = {message: string, severity: 'info' | 'success' | 'error'};

export type {
  Config,
  FeedbackMessage,
  Executor,
  WorkflowDefinition,
  WorkflowInstance,
  WorkflowInstanceAction,
  WorkflowStatistics,
  WorkflowSummaryStatistics,
  NewWorkflowInstance,
  NewWorkflowInstanceResponse,
};
