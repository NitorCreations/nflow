alter table nflow_workflow alter status drop default;

update nflow_workflow set status = 'finished' where next_activation is null and status = 'inProgress';
