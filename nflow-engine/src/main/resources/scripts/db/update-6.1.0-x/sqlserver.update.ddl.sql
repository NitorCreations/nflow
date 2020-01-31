if exists (select 1 from sys.indexes where name='nflow_archive_workflow_root')
drop index nflow_archive_workflow_root;

alter table nflow_workflow drop constraint fk_workflow_parent;

alter table nflow_workflow drop constraint fk_workflow_root;
  
alter table nflow_workflow drop column workflow_root_id;

alter table nflow_archive_workflow drop column workflow_root_id;

create index idx_workflow_parent on nflow_workflow (parent_workflow_id);

create index idx_workflow_archive_parent on nflow_archive_workflow (parent_workflow_id);
