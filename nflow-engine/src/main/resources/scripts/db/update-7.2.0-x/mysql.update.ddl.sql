alter table nflow_archive_workflow modify column retries int not null;
alter table nflow_archive_workflow modify column created timestamp(3);
alter table nflow_archive_workflow modify column modified timestamp(3);

alter table nflow_archive_workflow_action modify column execution_start timestamp(3);
alter table nflow_archive_workflow_action modify column execution_end timestamp(3);
