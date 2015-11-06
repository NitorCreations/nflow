alter table nflow_workflow add root_workflow_id integer default null;
alter table nflow_workflow add parent_workflow_id integer default null;
alter table nflow_workflow add parent_action_id integer default null;
alter table nflow_workflow add external_next_activation timestamp default null,

alter table nflow_workflow add constraint fk_workflow_parent
  foreign key (parent_workflow_id, parent_action_id) references nflow_workflow_action (workflow_id, id) on delete cascade;

alter table nflow_workflow add constraint fk_workflow_root
  foreign key (root_workflow_id) references nflow_workflow (id) on delete cascade;

alter table nflow_executor alter column host varchar(253) not null;

-- archiving
drop index nflow_workflow_next_activation;
create index if not exists nflow_workflow_next_activation on nflow_workflow(next_activation, modified);

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

create index if not exists nflow_archive_workflow_next_activation on nflow_archive_workflow(next_activation, modified);

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
