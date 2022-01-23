alter table nflow_archive_workflow drop index nflow_archive_workflow_uniq;

create index idx_workflow_archive_type on nflow_archive_workflow(type);

alter table nflow_archive_workflow ROW_FORMAT=Compressed;
alter table nflow_archive_workflow_state ROW_FORMAT=Compressed;
alter table nflow_archive_workflow_action ROW_FORMAT=Compressed;
