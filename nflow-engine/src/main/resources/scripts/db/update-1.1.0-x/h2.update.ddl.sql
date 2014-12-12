create table if not exists nflow_workflow_definition (
  type varchar(64) not null,
  definition text not null,
  created timestamp not null default current_timestamp,
  modified timestamp not null default current_timestamp,
  modified_by int not null,
  executor_group varchar(64) not null,
  primary key (type, executor_group)
);
