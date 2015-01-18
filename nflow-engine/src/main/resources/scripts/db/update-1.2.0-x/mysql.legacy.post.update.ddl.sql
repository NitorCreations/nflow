alter table nflow_workflow modify status enum('created', 'inProgress', 'finished', 'manual') not null;

update nflow_workflow set status = 'finished' where next_activation is null and status = 'inProgress';
