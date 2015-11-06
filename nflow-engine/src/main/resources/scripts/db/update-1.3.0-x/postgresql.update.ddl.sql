alter table nflow_workflow add root_workflow_id integer default null;
alter table nflow_workflow add parent_workflow_id integer default null;
alter table nflow_workflow add parent_action_id integer default null;
alter table nflow_workflow add external_next_activation timestamptz default null;

alter table nflow_workflow add constraint fk_workflow_parent
  foreign key (parent_workflow_id, parent_action_id) references nflow_workflow_action (workflow_id, id) on delete cascade;

alter table nflow_workflow add constraint fk_workflow_root
  foreign key (root_workflow_id) references nflow_workflow (id) on delete cascade;

alter table nflow_executor alter host varchar(253) not null;

-- archiving

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

drop index nflow_archive_workflow_activation;
create index nflow_archive_workflow_activation on nflow_archive_workflow(next_activation, modified);

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
