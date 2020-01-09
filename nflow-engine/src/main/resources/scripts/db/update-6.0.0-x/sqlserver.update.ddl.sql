drop index nflow_workflow_parent on nflow_workflow;
create index nflow_workflow_parent on nflow_workflow(parent_workflow_id, parent_action_id) where parent_workflow_id is not null;
