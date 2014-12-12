create table if not exists nflow_workflow_definition (
  type varchar(64) not null,
  definition text not null,
  created timestamptz not null default current_timestamp,
  modified timestamptz not null default current_timestamp,
  modified_by int not null,
  executor_group varchar(64) not null,
  primary key (type, executor_group)
);

drop trigger if exists update_nflow_definition_modified on nflow_workflow_definition;
create trigger update_nflow_definition_modified before update on nflow_workflow_definition for each row execute procedure update_modified();
