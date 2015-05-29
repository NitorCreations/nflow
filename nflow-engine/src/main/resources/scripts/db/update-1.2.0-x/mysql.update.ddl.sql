alter table nflow_workflow_definition add definition_sha1 varchar(40) not null default 'n/a';

alter table nflow_workflow_action add type enum('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange') not null default 'stateExecution';

alter table nflow_workflow add status enum('created', 'executing', 'inProgress', 'finished', 'manual') not null default 'inProgress';
