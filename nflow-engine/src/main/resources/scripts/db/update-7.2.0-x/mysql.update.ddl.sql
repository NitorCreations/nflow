alter table nflow_archive_workflow alter column retries drop default;
alter table nflow_archive_workflow alter column created drop default;
alter table nflow_archive_workflow alter column modified drop default;

alter table nflow_archive_workflow_action alter column execution_start drop default;
alter table nflow_archive_workflow_action alter column execution_end drop default;
