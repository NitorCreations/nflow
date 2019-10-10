-- production tables

create table nflow_workflow (
  id int not null identity(1,1) primary key,
  status varchar(32) not null,
  type varchar(64) not null,
  root_workflow_id int default null,
  parent_workflow_id int default null,
  parent_action_id int default null,
  business_key varchar(64),
  external_id varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  next_activation datetimeoffset(3),
  external_next_activation datetimeoffset(3),
  executor_id int,
  retries int not null default 0,
  created datetimeoffset(3) not null default SYSDATETIMEOFFSET(),
  modified datetimeoffset(3) not null default SYSDATETIMEOFFSET(),
  started datetimeoffset(3),
  executor_group varchar(64) not null,
  workflow_signal int,
  constraint nflow_workflow_uniq unique (type, external_id, executor_group)
);

create trigger nflow_workflow_modified_trigger on nflow_workflow after update as
begin
  update nflow_workflow set modified = SYSDATETIMEOFFSET()
  from nflow_workflow wf inner join inserted i on wf.id = i.id
end;

drop index nflow_workflow_activation;
create index nflow_workflow_activation on nflow_workflow(next_activation, modified);

create index nflow_workflow_polling on nflow_workflow(next_activation, status, executor_id, executor_group);

create table nflow_workflow_action (
  id int not null identity(1,1) primary key,
  workflow_id int not null,
  executor_id int not null default -1,
  type varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start datetimeoffset(3) not null,
  execution_end datetimeoffset(3) not null,
  foreign key (workflow_id) references nflow_workflow(id) on delete cascade,
  constraint nflow_workflow_action_uniq unique (workflow_id, id)
);

create index nflow_workflow_action_workflow on nflow_workflow_action(workflow_id);

alter table nflow_workflow add constraint fk_workflow_parent
  foreign key (parent_workflow_id, parent_action_id) references nflow_workflow_action (workflow_id, id) on update no action;

create index nflow_workflow_parent on nflow_workflow(parent_workflow_id, parent_action_id);

alter table nflow_workflow add constraint fk_workflow_root
  foreign key (root_workflow_id) references nflow_workflow (id) on update no action;

create index nflow_workflow_root on nflow_workflow(root_workflow_id);

create table nflow_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value text not null,
  primary key (workflow_id, action_id, state_key),
  foreign key (workflow_id) references nflow_workflow(id) on delete cascade
);

create index nflow_workflow_state_workflow on nflow_workflow_state(workflow_id);

create table nflow_executor (
  id int not null identity(1,1) primary key,
  host varchar(253) not null,
  pid int not null,
  executor_group varchar(64),
  started datetimeoffset(3) not null default SYSDATETIMEOFFSET(),
  active datetimeoffset(3) not null,
  expires datetimeoffset(3) not null,
  stopped datetimeoffset(3)
);

create table nflow_workflow_definition (
  type varchar(64) not null,
  definition_sha1 varchar(40) not null,
  definition text not null,
  created datetimeoffset(3) not null default SYSDATETIMEOFFSET(),
  modified datetimeoffset(3) not null default SYSDATETIMEOFFSET(),
  modified_by int not null,
  executor_group varchar(64) not null,
  primary key (type, executor_group)
);

create trigger nflow_workflow_definition_modified_trigger on nflow_workflow_definition after update as
begin
  update nflow_workflow_definition set modified = SYSDATETIMEOFFSET()
  from nflow_workflow_definition df inner join inserted i on df.type = i.type and df.executor_group = i.executor_group
end;


-- Archive tables
-- - no default values
-- - no triggers
-- - no auto increments
-- - same indexes and constraints as production tables
-- - remove recursive foreign keys

create table nflow_archive_workflow (
  id int not null primary key,
  status varchar(32) not null,
  type varchar(64) not null,
  root_workflow_id int,
  parent_workflow_id int,
  parent_action_id int,
  business_key varchar(64),
  external_id varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  next_activation datetimeoffset(3),
  external_next_activation datetimeoffset(3),
  executor_id int,
  retries int not null default 0,
  created datetimeoffset(3) not null,
  modified datetimeoffset(3) not null,
  started datetimeoffset(3),
  executor_group varchar(64) not null,
  workflow_signal int,
  constraint nflow_archive_workflow_uniq unique (type, external_id, executor_group)
);

create index nflow_archive_workflow_activation on nflow_archive_workflow(next_activation, modified);

create index nflow_archive_workflow_polling on nflow_archive_workflow(next_activation, status, executor_id, executor_group);

create index nflow_archive_workflow_parent on nflow_archive_workflow(parent_workflow_id, parent_action_id);

create index nflow_archive_workflow_root on nflow_archive_workflow(root_workflow_id);

create table nflow_archive_workflow_action (
  id int not null primary key,
  workflow_id int not null,
  executor_id int not null,
  type varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start datetimeoffset(3) not null,
  execution_end datetimeoffset(3) not null,
  foreign key (workflow_id) references nflow_archive_workflow(id) on delete cascade,
  constraint nflow_archive_workflow_action_uniq unique (workflow_id, id)
);

create index nflow_archive_workflow_action_workflow on nflow_archive_workflow_action(workflow_id);

create table nflow_archive_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value text not null,
  primary key (workflow_id, action_id, state_key),
  foreign key (workflow_id) references nflow_archive_workflow(id) on delete cascade
);

create index nflow_archive_workflow_state_workflow on nflow_archive_workflow_state(workflow_id);
