alter table nflow_workflow_definition add definition_sha1 varchar(40) not null default 'n/a';
alter table nflow_workflow_action add type enum('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange') not null default 'stateExecution';

