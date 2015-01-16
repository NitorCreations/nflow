create type action_type as enum ('stateExecution', 'stateExecutionFailed', 'recovery', 'manualStateChange');
alter table nflow_workflow_action add type action_type not null default 'stateExecution';
