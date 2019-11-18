if not exists (select 1 from sys.indexes where name='nflow_workflow_activation')
create index nflow_workflow_activation on nflow_workflow(next_activation, modified);
