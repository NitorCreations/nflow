drop index idx_workflow_parent;
create index concurrently idx_workflow_parent on nflow_workflow(parent_workflow_id) where parent_workflow_id is not null;

drop index idx_workflow_archive_parent;
create index concurrently idx_workflow_archive_parent on nflow_archive_workflow(parent_workflow_id) where parent_workflow_id is not null;

alter table nflow_workflow set (fillfactor=95);

alter index nflow_workflow_action_pkey set (fillfactor=100);
alter index nflow_workflow_action_workflow set (fillfactor=100);
alter index pk_workflow_state set (fillfactor=100);

drop index nflow_archive_workflow_uniq;

alter index nflow_archive_workflow_pkey set (fillfactor=100);
alter index idx_workflow_archive_parent set (fillfactor=100);
alter index nflow_archive_workflow_action_pkey set (fillfactor=100);
alter index nflow_archive_workflow_action_workflow set (fillfactor=100);
alter index pk_arch_workflow_state set (fillfactor=100);

alter table nflow_worklow rename constraint nflow_workflow_uniq to nflow_workflow_uniq_old;
alter table nflow_worklow add constraint nflow_workflow_uniq unique (external_id, type, executor_group);
alter table nflow_worklow drop constraint nflow_workflow_uniq_old;

create index idx_workflow_archive_type on nflow_archive_workflow(type) with (fillfactor=100);