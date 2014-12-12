create table if not exists nflow_workflow_definition (
  type varchar(64) not null,
  definition text not null,
  modified timestamp not null default current_timestamp on update current_timestamp,
  modified_by int not null,
  created timestamp not null,
  executor_group varchar(64) not null,
  primary key (type, executor_group)
);

drop trigger if exists nflow_workflow_definition_insert;

create trigger nflow_workflow_definition_insert before insert on `nflow_workflow_definition`
  for each row set new.created = now();
