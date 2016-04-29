alter table nflow_executor modify active timestamptz not null;
alter table nflow_executor modify expires timestamptz not null;
