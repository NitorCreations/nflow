interface Endpoint {
  id: string;
  title: string;
  apiUrl: string;
  docUrl: string;
}

interface Config {
  refreshSeconds: number;
  activeNflowEndpoint: Endpoint;
  nflowEndpoints: Array<Endpoint>;
  customInstanceContent: (
    definition: WorkflowDefinition,
    workflow: WorkflowInstance,
    parentWorkflow: WorkflowInstance | undefined,
    childWorkflows: Array<WorkflowInstance>
  ) => void;
  htmlTitle?: string;
  nflowLogoFile?: string;
  nflowLogoTitle?: string;
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
  type: 'start' | 'normal' | 'wait' | 'manual' | 'end';
  description?: string;
  transitions: Array<string>;
  onFailure?: string;
}

interface WorkflowSignal {
  value: number;
  description: string;
}

interface WorkflowDefinition {
  type: string;
  description?: string;
  onError: string;
  states: Array<WorkflowState>;
  supportedSignals: Array<WorkflowSignal>;
  settings: any;
}

interface WorkflowInstanceAction {
  id: number;
  workflowInstanceId: number;
  executorId: number;
  type:
    | 'stateExecution'
    | 'stateExecutionFailed'
    | 'externalChange'
    | 'recovery';
  state: string;
  stateText?: string;
  updatedStateVariables: {[key: string]: string};
  retryNo: number;
  executionStartTime: Date;
  executionEndTime: Date;
}

interface WorkflowInstance {
  id: number;
  type: string;
  status: 'created' | 'inProgress' | 'executing' | 'manual' | 'finished';
  state: string;
  stateText?: string;
  nextActivation?: Date;
  stateVariables: {[key: string]: string};
  originalVariables: {[key: string]: string};
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
  signal?: number;
}

interface StateStatistics {
  created: {allInstances: number; queuedInstances: 0};
  inProgress: {allInstances: number; queuedInstances: 0};
  executing: {allInstances: number};
  manual: {allInstances: number};
  finished: {allInstances: number};
}
interface WorkflowStatistics {
  [key: string]: StateStatistics;
}

interface WorkflowSummaryStatistics {
  stats: Array<{
    state: string;
    stats: {[status: string]: {allInstances: number; queuedInstances?: 0}};
    total: number;
  }>;
  totalPerStatus: {
    [status: string]: {allInstances: number; queuedInstances?: 0};
  };
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
  type: string;
  externalId: string;
}

type FeedbackMessage = {
  message: string;
  severity: 'info' | 'success' | 'error';
};

export type {
  Config,
  FeedbackMessage,
  Executor,
  WorkflowDefinition,
  WorkflowInstance,
  WorkflowInstanceAction,
  WorkflowSignal,
  WorkflowState,
  WorkflowStatistics,
  WorkflowSummaryStatistics,
  NewWorkflowInstance,
  NewWorkflowInstanceResponse
};
