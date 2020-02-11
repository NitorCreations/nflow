-- Production tables

create table if not exists nflow_workflow (
  id int not null auto_increment primary key,
  status enum('created', 'executing', 'inProgress', 'finished', 'manual') not null,
  type varchar(64) not null,
  priority smallint not null default 0,
  parent_workflow_id integer default null,
  parent_action_id integer default null,
  business_key varchar(64),
  external_id varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  next_activation timestamp(3) null,
  external_next_activation timestamp(3) null,
  executor_id int,
  retries int not null default 0,
  created timestamp(3) default current_timestamp(3),
  modified timestamp(3) default current_timestamp(3) on update current_timestamp(3),
  started timestamp(3) null,
  executor_group varchar(64) not null,
  workflow_signal int,
  constraint nflow_workflow_uniq unique (type, external_id, executor_group)
);

create index nflow_workflow_polling on nflow_workflow(next_activation, status, executor_id, executor_group);

create index idx_workflow_parent on nflow_workflow(parent_workflow_id);

create table if not exists nflow_workflow_action (
  id int not null auto_increment primary key,
  workflow_id int not null,
  executor_id int not null default -1,
  type enum('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange') not null,
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start timestamp(3) default current_timestamp(3),
  execution_end timestamp(3) default current_timestamp(3),
  constraint fk_action_workflow_id foreign key (workflow_id) references nflow_workflow(id)
);

create index nflow_workflow_action_workflow on nflow_workflow_action(workflow_id);

create table if not exists nflow_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value varchar(10240) not null,
  constraint pk_workflow_state primary key (workflow_id, action_id, state_key),
  constraint fk_state_workflow_id foreign key (workflow_id) references nflow_workflow(id)
);

create table if not exists nflow_executor (
  id int not null auto_increment primary key,
  host varchar(253) not null,
  pid int not null,
  executor_group varchar(64),
  started timestamp(3) not null default current_timestamp(3),
  active timestamp(3) not null default current_timestamp(3),
  expires timestamp(3) not null default current_timestamp(3),
  stopped timestamp(3)
);

create table if not exists nflow_workflow_definition (
  type varchar(64) not null,
  definition_sha1 varchar(40) not null,
  definition text not null,
  created timestamp(3) default current_timestamp(3),
  modified timestamp(3) default current_timestamp(3) on update current_timestamp(3),
  modified_by int not null,
  executor_group varchar(64) not null,
  constraint pk_workflow_definition primary key (type, executor_group)
);

-- Archive tables
-- - no default values
-- - no triggers
-- - no auto increments
-- - same indexes and constraints as production tables
-- - remove recursive foreign keys

create table if not exists nflow_archive_workflow (
  id int not null primary key,
  status enum('created', 'executing', 'inProgress', 'finished', 'manual') not null,
  type varchar(64) not null,
  priority smallint null,
  parent_workflow_id integer,
  parent_action_id integer,
  business_key varchar(64),
  external_id varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  next_activation timestamp(3) null,
  external_next_activation timestamp(3) null,
  executor_id int,
  retries int not null default 0,
  created timestamp(3) default current_timestamp(3),
  modified timestamp(3) default current_timestamp(3),
  started timestamp(3) null,
  executor_group varchar(64) not null,
  workflow_signal int,
  constraint nflow_archive_workflow_uniq unique (type, external_id, executor_group)
);

create index idx_workflow_archive_parent on nflow_archive_workflow(parent_workflow_id);

create table if not exists nflow_archive_workflow_action (
  id int not null primary key,
  workflow_id int not null,
  executor_id int not null,
  type enum('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange') not null,
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start timestamp(3) default current_timestamp(3),
  execution_end timestamp(3) default current_timestamp(3),
  constraint fk_arch_action_wf_id foreign key (workflow_id) references nflow_archive_workflow(id)
);

create index nflow_archive_workflow_action_workflow on nflow_archive_workflow_action(workflow_id);

create table if not exists nflow_archive_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value varchar(10240) not null,
  constraint pk_arch_workflow_state primary key (workflow_id, action_id, state_key),
  constraint fk_arch_state_wf_id foreign key (workflow_id) references nflow_archive_workflow(id)
);
