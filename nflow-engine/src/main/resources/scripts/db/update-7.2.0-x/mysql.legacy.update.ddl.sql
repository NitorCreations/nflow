alter table nflow_archive_workflow alter column retries drop default;

alter table nflow_archive_workflow_action alter column executor_id drop default;
