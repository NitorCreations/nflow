alter table nflow_workflow_definition add definition_sha1 varchar(40) not null default 'n/a';

create type action_type as enum ('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange');
alter table nflow_workflow_action add type action_type not null default 'stateExecution';

create type workflow_status as enum ('created', 'executing', 'inProgress', 'finished', 'manual');
alter table nflow_workflow add status workflow_status not null default 'inProgress';
