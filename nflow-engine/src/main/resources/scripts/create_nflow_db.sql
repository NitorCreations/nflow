create table nflow_workflow (
  id int not null auto_increment primary key,
  type varchar(32) not null,
  business_key varchar(64),
  request_data varchar(1024), 
  state varchar(32) not null,
  state_text varchar(128),
  next_activation timestamp,
  is_processing boolean not null default false,
  retries int not null default 0,
  created timestamp not null default current_timestamp,
  modified timestamp not null default current_timestamp,
  owner varchar(64)
);

create table nflow_workflow_action (
  id int not null auto_increment primary key,
  workflow_id int not null,
  created timestamp not null default current_timestamp,
  state_next varchar(32) not null,
  state_next_text varchar(128),
  next_activation timestamp
);

create table nflow_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value varchar(1024) not null,
  primary key (workflow_id, action_id, state_key)
);