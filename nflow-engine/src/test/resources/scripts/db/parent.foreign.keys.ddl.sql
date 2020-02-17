alter table nflow_workflow add constraint if not exists fk_workflow_parent
  foreign key (parent_workflow_id, parent_action_id) references nflow_workflow_action (workflow_id, id);
alter table nflow_workflow add constraint if not exists fk_workflow_root
  foreign key (root_workflow_id) references nflow_workflow (id);

create index nflow_workflow_parent on nflow_workflow (parent_workflow_id);
create index nflow_workflow_root on nflow_workflow (root_workflow_id);
