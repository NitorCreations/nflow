create table if not exists nflow_workflow (
  id serial primary key,
  type varchar(64) not null,
  business_key varchar(64),
  external_id varchar(64),
  request_data varchar(1024), 
  state varchar(64) not null,
  state_text varchar(128),
  next_activation timestamptz,
  is_processing boolean not null default false,
  retries int not null default 0,
  created timestamptz not null default current_timestamp,
  modified timestamptz not null default current_timestamp,
  owner varchar(64),
  constraint nflow_workflow_uniq unique (type, external_id)
);

create or replace function update_modified() returns trigger language plpgsql as '
begin
  NEW.modified := now();
  return NEW;
end;
';

drop trigger if exists update_nflow_modified on nflow_workflow;
create trigger update_nflow_modified before update on nflow_workflow for each row execute procedure update_modified();

drop index nflow_workflow_activation;
create index nflow_workflow_activation on nflow_workflow(next_activation) where next_activation is not null;

create table if not exists nflow_workflow_action (
  id serial primary key,
  workflow_id int not null,
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start timestamptz not null,
  execution_end timestamptz not null,
  foreign key (workflow_id) references nflow_workflow(id) on delete cascade,
  constraint nflow_workflow_action_uniq unique (workflow_id, id)
);

create table if not exists nflow_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value text not null,
  primary key (workflow_id, action_id, state_key),
  foreign key (workflow_id) references nflow_workflow(id) on delete cascade
);