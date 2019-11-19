create index nflow_workflow_polling on nflow_workflow(next_activation, status, executor_id, executor_group);

create index nflow_workflow_parent on nflow_workflow(parent_workflow_id, parent_action_id);

create index nflow_workflow_root on nflow_workflow(root_workflow_id);

create index nflow_workflow_action_workflow on nflow_workflow_action(workflow_id);

create index nflow_workflow_state_workflow on nflow_workflow_state(workflow_id);

create index nflow_archive_workflow_parent on nflow_archive_workflow(parent_workflow_id, parent_action_id);

create index nflow_archive_workflow_root on nflow_archive_workflow(root_workflow_id);

create index nflow_archive_workflow_action_workflow on nflow_archive_workflow_action(workflow_id);

create index nflow_archive_workflow_state_workflow on nflow_archive_workflow_state(workflow_id);
