alter table nflow_workflow_action add type enum('stateExecution', 'stateExecutionFailed', 'recovery', 'manualStateChange') not null default 'stateExecution';
