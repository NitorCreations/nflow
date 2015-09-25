alter table nflow_workflow add parent_workflow_id integer default null;
alter table nflow_workflow add parent_action_id integer default null;
alter table nflow_workflow add external_next_activation timestamp default null,

alter table nflow_workflow add constraint fk_workflow_parent
  foreign key (parent_workflow_id, parent_action_id) references nflow_workflow_action (workflow_id, id) on delete cascade;

alter table nflow_executor alter column host varchar(253) not null;
alter table nflow_workflow_action modify type varchar(32) not null check type in ('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange', 'executionFilterUpdate');
