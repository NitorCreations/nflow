drop index if exists nflow_workflow_polling;
create index nflow_workflow_polling on nflow_workflow(next_activation, status, executor_id, executor_group);
