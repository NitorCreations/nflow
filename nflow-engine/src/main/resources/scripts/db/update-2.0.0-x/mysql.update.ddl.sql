alter table nflow_executor modify active timestamp(3) not null default current_timestamp(3);
alter table nflow_executor modify expires timestamp(3) not null default current_timestamp(3);
