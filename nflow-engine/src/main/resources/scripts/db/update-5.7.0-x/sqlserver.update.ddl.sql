alter table nflow_workflow add priority smallint not null default 0;
alter table nflow_archive_workflow add priority smallint null;

create index nflow_workflow_polling on nflow_workflow(next_activation, status, executor_id, executor_group) where next_activation is not null;

create index nflow_workflow_parent on nflow_workflow(parent_workflow_id, parent_action_id);

create index nflow_workflow_root on nflow_workflow(root_workflow_id);

create index nflow_workflow_action_workflow on nflow_workflow_action(workflow_id);

create index nflow_workflow_state_workflow on nflow_workflow_state(workflow_id);

create index nflow_archive_workflow_parent on nflow_archive_workflow(parent_workflow_id, parent_action_id);

create index nflow_archive_workflow_root on nflow_archive_workflow(root_workflow_id);

create index nflow_archive_workflow_action_workflow on nflow_archive_workflow_action(workflow_id);

create index nflow_archive_workflow_state_workflow on nflow_archive_workflow_state(workflow_id);
