create index nflow_workflow_polling on nflow_workflow(next_activation, status, executor_id, executor_group);

drop index nflow_workflow_activation on nflow_workflow;
