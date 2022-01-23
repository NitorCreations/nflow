alter table nflow_workflow_state modify column state_value varchar(max) not null;

alter table nflow_workflow_definition modify column definition varchar(max) not null;

alter table nflow_workflow_state modify column state_value varchar(max) not null;
