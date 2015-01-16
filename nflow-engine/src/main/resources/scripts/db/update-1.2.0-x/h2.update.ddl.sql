alter table nflow_workflow_definition add definition_sha1 varchar(40) not null default 'n/a';

alter table nflow_workflow_action add type varchar(32) not null default 'stateExecution' check type in ('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange');

