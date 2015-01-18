alter table nflow_workflow alter column status set default null;

update nflow_workflow set status = 'finished' where next_activation is null and status = 'inProgress';
