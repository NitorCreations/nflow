-- production tables

create table nflow_workflow (
  id int primary key generated always as identity,
  status varchar(32) not null check (status in ('created', 'executing', 'inProgress', 'finished', 'manual')),
  type varchar(64) not null,
  priority int not null default 0,
  root_workflow_id integer default null,
  parent_workflow_id integer default null,
  parent_action_id integer default null,
  business_key varchar(64),
  external_id varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  next_activation timestamp(3),
  external_next_activation timestamp(3),
  executor_id int,
  retries int not null default 0,
  created timestamp(3) not null default current_timestamp,
  modified timestamp(3) not null default current_timestamp,
  started timestamp(3),
  executor_group varchar(64) not null,
  workflow_signal int,
  constraint nflow_workflow_uniq unique (type, external_id, executor_group)
);

create or replace trigger nflow_workflow_update_modified
  before update on nflow_workflow
  referencing new as n
  for each row
  set modified = current timestamp;

create index nflow_workflow_polling on nflow_workflow(next_activation, status, executor_id, executor_group);

create table nflow_workflow_action (
  id int primary key generated always as identity,
  workflow_id int not null,
  executor_id int not null default -1,
  type varchar(32) not null check (type in ('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange')),
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start timestamp(3) not null,
  execution_end timestamp(3) not null,
  foreign key (workflow_id) references nflow_workflow(id) on delete cascade,
  constraint nflow_workflow_action_uniq unique (workflow_id, id)
);

alter table nflow_workflow add constraint fk_workflow_parent
  foreign key (parent_workflow_id, parent_action_id) references nflow_workflow_action (workflow_id, id) on delete cascade;

alter table nflow_workflow add constraint fk_workflow_root
  foreign key (root_workflow_id) references nflow_workflow (id) on delete cascade;

create table nflow_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value varchar(10240) not null,
  primary key (workflow_id, action_id, state_key),
  foreign key (workflow_id) references nflow_workflow(id) on delete cascade
);

create table nflow_executor (
  id int primary key generated always as identity,
  host varchar(253) not null,
  pid int not null,
  executor_group varchar(64),
  started timestamp(3) not null default current_timestamp,
  active timestamp(3) not null,
  expires timestamp(3) not null,
  stopped timestamp(3)
);

create table nflow_workflow_definition (
  type varchar(64) not null,
  definition_sha1 varchar(40) not null,
  definition varchar(10240) not null,
  created timestamp(3) not null default current_timestamp,
  modified timestamp(3) not null default current_timestamp,
  modified_by int not null,
  executor_group varchar(64) not null,
  primary key (type, executor_group)
);

create or replace trigger nflow_workflow_definition_update_modified
  before update on nflow_workflow_definition
  referencing new as n
  for each row
  set modified = current timestamp;

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
  root_workflow_id integer,
  parent_workflow_id integer,
  parent_action_id integer,
  business_key varchar(64),
  external_id varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  next_activation timestamp(3),
  external_next_activation timestamp(3),
  executor_id int,
  retries int not null default 0,
  created timestamp(3) not null,
  modified timestamp(3) not null,
  started timestamp(3),
  executor_group varchar(64) not null,
  workflow_signal int,
  constraint nflow_archive_workflow_uniq unique (type, external_id, executor_group)
);

create table nflow_archive_workflow_action (
  id int not null primary key,
  workflow_id int not null,
  executor_id int not null,
  type varchar(32)  not null,
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start timestamp(3) not null,
  execution_end timestamp(3) not null,
  foreign key (workflow_id) references nflow_archive_workflow(id) on delete cascade,
  constraint nflow_archive_workflow_action_uniq unique (workflow_id, id)
);

create table nflow_archive_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value varchar(10240) not null,
  primary key (workflow_id, action_id, state_key),
  foreign key (workflow_id) references nflow_archive_workflow(id) on delete cascade
);
