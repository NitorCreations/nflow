alter table nflow_workflow_action add type varchar(32) not null default 'stateExecution' check type in ('stateExecution', 'stateExecutionFailed', 'recovery', 'manualStateChange');
