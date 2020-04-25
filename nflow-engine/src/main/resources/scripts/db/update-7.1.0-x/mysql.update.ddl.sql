drop index nflow_archive_workflow_uniq;

alter table nflow_archive_workflow ROW_FORMAT=Compressed;
alter table nflow_archive_workflow_state ROW_FORMAT=Compressed;
alter table nflow_archive_workflow_action ROW_FORMAT=Compressed;
