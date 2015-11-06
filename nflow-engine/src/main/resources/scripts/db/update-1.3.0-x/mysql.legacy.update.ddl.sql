alter table nflow_workflow add root_workflow_id integer default null;
alter table nflow_workflow add parent_workflow_id integer default null;
alter table nflow_workflow add parent_action_id integer default null;
alter table nflow workflow add external_next_activation timestamp default null;

alter table nflow_workflow add constraint fk_workflow_parent
  foreign key (parent_workflow_id, parent_action_id) references nflow_workflow_action (workflow_id, id) on delete cascade;

alter table nflow_workflow add constraint fk_workflow_root
  foreign key (root_workflow_id) references nflow_workflow (id) on delete cascade;

alter table nflow_executor modify host varchar(253) not null;

-- archiving

alter table drop nflow_workflow index nflow_workflow;
create index nflow_workflow_activation on nflow_workflow(next_activation, modified);

create table if not exists nflow_archive_workflow (
  id int not null primary key,
  status enum('created', 'executing', 'inProgress', 'finished', 'manual') not null,
  type varchar(64) not null,
  root_workflow_id integer,
  parent_workflow_id integer,
  parent_action_id integer,
  business_key varchar(64),
  external_id varchar(64) not null,
  state varchar(64) not null,
  state_text varchar(128),
  next_activation timestamp null,
  external_next_activation timestamp null,
  executor_id int,
  retries int not null default 0,
  created timestamp not null,
  modified timestamp not null,
  executor_group varchar(64) not null,
  constraint nflow_archive_workflow_uniq unique (type, external_id, executor_group),
  index nflow_archive_workflow(next_activation, modified)
);

create table if not exists nflow_archive_workflow_action (
  id int not null primary key,
  workflow_id int not null,
  executor_id int not null default -1,
  type enum('stateExecution', 'stateExecutionFailed', 'recovery', 'externalChange') not null,
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
