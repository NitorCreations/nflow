create table nflow_workflow (
  id int not null primary key,
  status varchar(32) not null,
  type varchar(64) not null,
  parent_workflow_id int default null,
  parent_action_id int default null,
  business_key varchar(64),
  external_id varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  next_activation timestamp null,
  external_next_activation timestamp null,
  executor_id int,
  retries int default 0 not null,
  created timestamp default current_timestamp not null,
  modified timestamp default current_timestamp not null,
  executor_group varchar(64) not null,
  constraint nflow_workflow_uniq unique (type, external_id, executor_group),
  constraint status_enum check (status in ('created', 'executing', 'inProgress', 'finished', 'manual'))
);

create index nflow_workflow_activation on nflow_workflow (next_activation);

create sequence nflow_workflow_id_seq;

create or replace trigger nflow_workflow_insert
  before insert on nflow_workflow
  for each row
declare
begin
  :new.id := nflow_workflow_id_seq.nextval;
end;
/

create or replace trigger nflow_workflow_update
  before update on nflow_workflow
  for each row
declare
begin
  :new.modified := current_timestamp;
end;
/

create table nflow_workflow_action (
  id int not null primary key,
  workflow_id int not null,
  executor_id int default -1 not null,
  type varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start timestamp not null,
  execution_end timestamp not null,
  foreign key (workflow_id) references nflow_workflow(id) on delete cascade,
  constraint nflow_workflow_action_uniq unique (workflow_id, id),
  constraint type_enum check (type in ('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange'))
);

create sequence nflow_workflow_action_id_seq;

create or replace trigger nflow_workflow_action_insert
  before insert on nflow_workflow_action
  for each row
declare
begin
  :new.id := nflow_workflow_id_seq.nextval;
end;
/

alter table nflow_workflow add constraint fk_workflow_parent
  foreign key (parent_workflow_id, parent_action_id) references nflow_workflow_action (workflow_id, id) on delete cascade;

create table nflow_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value clob not null,
  primary key (workflow_id, action_id, state_key),
  foreign key (workflow_id) references nflow_workflow(id) on delete cascade
);

create table nflow_executor (
  id int not null primary key,
  host varchar(253) not null,
  pid int not null,
  executor_group varchar(64),
  started timestamp default current_timestamp not null,
  active timestamp,
  expires timestamp
);

create sequence nflow_executor_id_seq;

create or replace trigger nflow_executor_insert
  before insert on nflow_executor
  for each row
declare
begin
  :new.id := nflow_executor_id_seq.nextval;
end;
/

create table nflow_workflow_definition (
  type varchar(64) not null,
  definition_sha1 varchar(40) not null,
  definition clob not null,
  created timestamp default current_timestamp not null,
  modified timestamp default current_timestamp not null,
  modified_by int not null,
  executor_group varchar(64) not null,
  primary key (type, executor_group)
);

create or replace trigger nflow_workflow_def_update
  before update on nflow_workflow_definition
  for each row
declare
begin
  :new.modified := current_timestamp;
end;
/

-- Archive tables
-- - no default values
-- - no triggers
-- - no auto increments
-- - same indexes and constraints as production tables
-- - remove recursive foreign keys

create table nflow_archive_workflow (
  id integer primary key,
  status varchar(32) not null,
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
  retries int not null default 0,
  created timestamp not null,
  modified timestamp not null,
  executor_group varchar(64) not null,
  constraint nflow_archive_workflow_uniq unique (type, external_id, executor_group)
);

create table nflow_archive_workflow_action (
  id integer primary key,
  workflow_id int not null,
  executor_id int not null,
  type varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start timestamp not null,
  execution_end timestamp not null,
  foreign key (workflow_id) references nflow_archive_workflow(id) on delete cascade,
  constraint nflow_archive_workflow_action_uniq unique (workflow_id, id)
);

create table nflow_archive_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value clob not null,
  primary key (workflow_id, action_id, state_key),
  foreign key (workflow_id) references nflow_archive_workflow(id) on delete cascade
);

