alter table nflow_archive_workflow modify column retries int not null;

alter table nflow_archive_workflow_action modify column executor_id int not null;
