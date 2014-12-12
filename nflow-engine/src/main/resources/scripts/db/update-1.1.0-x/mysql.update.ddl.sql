create table if not exists nflow_workflow_definition (
  type varchar(64) not null,
  definition text not null,
  created timestamp(3) not null default current_timestamp(3),
  modified timestamp(3) not null default current_timestamp(3) on update current_timestamp(3),
  modified_by int not null,
  executor_group varchar(64) not null,
  primary key (type, executor_group)
);
