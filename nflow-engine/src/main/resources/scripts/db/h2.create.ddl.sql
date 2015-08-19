-- Production tables
create table if not exists nflow_workflow (
  id int not null auto_increment primary key,
  status varchar(32) not null check status in ('created', 'executing', 'inProgress', 'finished', 'manual'),
  type varchar(64) not null,
  root_workflow_id integer default null,
  parent_workflow_id integer default null,
  parent_action_id integer default null,
  business_key varchar(64),
  external_id varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  next_activation timestamp,
  external_next_activation timestamp,
  executor_id int,
  retries int not null default 0,
  created timestamp not null default current_timestamp,
  modified timestamp not null default current_timestamp,
  executor_group varchar(64) not null
);
create trigger if not exists nflow_workflow_modified before update on nflow_workflow for each row call "com.nitorcreations.nflow.engine.internal.storage.db.H2ModifiedColumnTrigger";

create unique index if not exists nflow_workflow_uniq on nflow_workflow (type, external_id, executor_group);

create index if not exists nflow_workflow_next_activation on nflow_workflow(next_activation, modified);

create table if not exists nflow_workflow_action (
  id int not null auto_increment primary key,
  workflow_id int not null,
  executor_id int not null default -1,
  type varchar(32) not null check type in ('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange'),
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start timestamp not null,
  execution_end timestamp not null,
  foreign key (workflow_id) references nflow_workflow(id) on delete cascade
);

alter table nflow_workflow add constraint fk_workflow_parent
  foreign key (parent_workflow_id, parent_action_id) references nflow_workflow_action (workflow_id, id) on delete cascade;

alter table nflow_workflow add constraint fk_workflow_root
  foreign key (root_workflow_id) references nflow_workflow (id) on delete cascade;

create table if not exists nflow_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value varchar(10240) not null,
  primary key (workflow_id, action_id, state_key),
  foreign key (workflow_id) references nflow_workflow(id) on delete cascade
);

create table if not exists nflow_executor (
  id int not null auto_increment primary key,
  host varchar(253) not null,
  pid int not null,
  executor_group varchar(64),
  started timestamp not null default current_timestamp,
  active timestamp,
  expires timestamp
);

create table if not exists nflow_workflow_definition (
  type varchar(64) not null,
  definition_sha1 varchar(40) not null,
  definition text not null,
  created timestamp not null default current_timestamp,
  modified timestamp not null default current_timestamp,
  modified_by int not null,
  executor_group varchar(64) not null,
  primary key (type, executor_group)
);

-- Archive tables
-- - no default values
-- - no triggers
-- - no auto increments
-- - same indexes and constraints as production tables
-- - remove recursive foreign keys

create table if not exists nflow_archive_workflow (
  id int not null primary key,
  status varchar(32) not null check status in ('created', 'executing', 'inProgress', 'finished', 'manual'),
  type varchar(64) not null,
  root_workflow_id integer,
  parent_workflow_id integer,
  parent_action_id integer,
  business_key varchar(64),
  external_id varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  next_activation timestamp,
  external_next_activation timestamp,
  executor_id int,
  retries int not null,
  created timestamp not null,
  modified timestamp not null,
  executor_group varchar(64) not null
);

create unique index if not exists nflow_archive_workflow_uniq on nflow_archive_workflow (type, external_id, executor_group);

create table if not exists nflow_archive_workflow_action (
  id int not null primary key,
  workflow_id int not null,
  executor_id int not null,
  type varchar(32) not null check type in ('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange'),
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start timestamp not null,
  execution_end timestamp not null,
  foreign key (workflow_id) references nflow_archive_workflow(id) on delete cascade
);

create table if not exists nflow_archive_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value varchar(10240) not null,
  primary key (workflow_id, action_id, state_key),
  foreign key (workflow_id) references nflow_archive_workflow(id) on delete cascade
);
