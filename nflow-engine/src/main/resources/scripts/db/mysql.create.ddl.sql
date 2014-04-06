create table nflow_workflow (
  id int not null auto_increment primary key,
  type varchar(64) not null,
  business_key varchar(64),
  external_id varchar(64),
  request_data varchar(1024), 
  state varchar(64) not null,
  state_text varchar(128),
  is_processing boolean not null default false,
  retries int not null default 0,
  modified timestamp not null default current_timestamp,
  next_activation timestamp null,
  created timestamp null,
  owner varchar(64)
);

CREATE TRIGGER nflow_workflow_create BEFORE INSERT ON `nflow_workflow`
  FOR EACH ROW SET NEW.created = NOW(), NEW.modified = NOW();

create unique index nflow_workflow_uniq ON nflow_workflow (type, external_id);

create table nflow_workflow_action (
  id int not null auto_increment primary key,
  workflow_id int not null,
  state varchar(64) not null,
  state_text varchar(128),
  retry_no int not null,
  execution_start timestamp not null,
  execution_end timestamp not null
);

create table nflow_workflow_state (
  workflow_id int not null,
  action_id int not null,
  state_key varchar(64) not null,
  state_value varchar(1024) not null,
  primary key (workflow_id, action_id, state_key)
);