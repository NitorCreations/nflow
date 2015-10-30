-- production tables

create type workflow_status as enum ('created', 'executing', 'inProgress', 'finished', 'manual');
create table if not exists nflow_workflow (
  id serial primary key,
  status workflow_status not null,
  type varchar(64) not null,
  root_workflow_id integer default null,
  parent_workflow_id integer default null,
  parent_action_id integer default null,
  business_key varchar(64),
  external_id varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  next_activation timestamptz,
  external_next_activation timestamptz,
  executor_id int,
  retries int not null default 0,
  created timestamptz not null default current_timestamp,
  modified timestamptz not null default current_timestamp,
  executor_group varchar(64) not null,
  constraint nflow_workflow_uniq unique (type, external_id, executor_group)
);

create or replace function update_modified() returns trigger language plpgsql as '
begin
  if NEW.modified = OLD.modified then
    NEW.modified := now();
  end if;
  return NEW;
end;
';

drop trigger if exists update_nflow_modified on nflow_workflow;
create trigger update_nflow_modified before update on nflow_workflow for each row execute procedure update_modified();

drop index nflow_workflow_activation;
create index nflow_workflow_activation on nflow_workflow(next_activation, modified);

create type action_type as enum ('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange');
create table if not exists nflow_workflow_action (
  id serial primary key,
  workflow_id int not null,
  executor_id int not null default -1,
  type action_type not null,
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start timestamptz not null,
  execution_end timestamptz not null,
  foreign key (workflow_id) references nflow_workflow(id) on delete cascade,
  constraint nflow_workflow_action_uniq unique (workflow_id, id)
);

alter table nflow_workflow add constraint fk_workflow_parent
  foreign key (parent_workflow_id, parent_action_id) references nflow_workflow_action (workflow_id, id) on delete cascade;

alter table nflow_workflow add constraint fk_workflow_root
  foreign key (root_workflow_id) references nflow_workflow (id) on delete cascade;

create table if not exists nflow_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value text not null,
  primary key (workflow_id, action_id, state_key),
  foreign key (workflow_id) references nflow_workflow(id) on delete cascade
);

create table if not exists nflow_executor (
  id serial primary key,
  host varchar(253) not null,
  pid int not null,
  executor_group varchar(64),
  started timestamptz not null default current_timestamp,
  active timestamptz,
  expires timestamptz
);

create table if not exists nflow_workflow_definition (
  type varchar(64) not null,
  definition_sha1 varchar(40) not null,
  definition text not null,
  created timestamptz not null default current_timestamp,
  modified timestamptz not null default current_timestamp,
  modified_by int not null,
  executor_group varchar(64) not null,
  primary key (type, executor_group)
);

drop trigger if exists update_nflow_definition_modified on nflow_workflow_definition;
create trigger update_nflow_definition_modified before update on nflow_workflow_definition for each row execute procedure update_modified();


-- Archive tables
-- - no default values
-- - no triggers
-- - no auto increments
-- - same indexes and constraints as production tables
-- - remove recursive foreign keys

create table if not exists nflow_archive_workflow (
  id integer primary key,
  status workflow_status not null,
  type varchar(64) not null,
  root_workflow_id integer,
  parent_workflow_id integer,
  parent_action_id integer,
  business_key varchar(64),
  external_id varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  next_activation timestamptz,
  external_next_activation timestamptz,
  executor_id int,
  retries int not null default 0,
  created timestamptz not null,
  modified timestamptz not null,
  executor_group varchar(64) not null,
  constraint nflow_archive_workflow_uniq unique (type, external_id, executor_group)
);

create table if not exists nflow_archive_workflow_action (
  id integer primary key,
  workflow_id int not null,
  executor_id int not null,
  type action_type not null,
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start timestamptz not null,
  execution_end timestamptz not null,
  foreign key (workflow_id) references nflow_archive_workflow(id) on delete cascade,
  constraint nflow_archive_workflow_action_uniq unique (workflow_id, id)
);

create table if not exists nflow_archive_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value text not null,
  primary key (workflow_id, action_id, state_key),
  foreign key (workflow_id) references nflow_archive_workflow(id) on delete cascade
);
