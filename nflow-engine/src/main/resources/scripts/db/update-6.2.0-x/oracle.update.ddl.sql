-- These can be run before nFlow is updated.

alter table nflow_workflow drop constraint fk_workflow_parent
/

alter table nflow_workflow drop constraint fk_workflow_root
/
  
create index idx_workflow_parent on nflow_workflow(parent_workflow_id)
/

create index nflow_workflow_action_workflow on nflow_workflow_action(workflow_id)
/

create index idx_workflow_archive_parent on nflow_archive_workflow(parent_workflow_id)
/

-- There must be run after nFlow is updated.

alter table nflow_workflow drop column workflow_root_id
/

alter table nflow_archive_workflow drop column workflow_root_id
/
