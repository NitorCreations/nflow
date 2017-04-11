alter table nflow_workflow add started timestamptz;
alter table nflow_archive_workflow add started timestamptz;

update table nflow_workflow set started = (select min(execution_start) from nflow_workflow_action where nflow_workflow_action.workflow_id = nflow_workflow.id)
update table nflow_archive_workflow set started = (select min(execution_start) from nflow_archive_workflow_action where nflow_archive_workflow_action.workflow_id = nflow_workflow.id)
