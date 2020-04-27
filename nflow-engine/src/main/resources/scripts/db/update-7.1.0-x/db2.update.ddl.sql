drop index nflow_archive_workflow_uniq;

create index idx_workflow_archive_type on nflow_archive_workflow(type);

