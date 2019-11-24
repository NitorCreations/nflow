alter table nflow_workflow add priority smallint not null default 0;
alter table nflow_archive_workflow add priority smallint null;

create index if not exists nflow_workflow_polling on nflow_workflow(next_activation, status, executor_id, executor_group);

drop index if exists nflow_workflow_next_activation;
