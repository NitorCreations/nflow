-- For reasonably small nflow_workflow row count:
alter table nflow_workflow add priority smallint not null default 0;
-- For large nflow_workflow row count:
--
--   alter table nflow_workflow add priority smallint null;
--   drop trigger if exists nflow_workflow_update_modified;
--
-- followed by either:
--
--   update nflow_workflow set priority = 0, modified = modified where priority is null;
--
-- or in batches of 100k with this query, repeated until no rows are affected:
--
--   update nflow_workflow set priority = 0, modified = modified where priority is null limit 100000;
--
-- and finally:
--
--   alter table nflow_workflow alter column priority smallint not null default 0;
--   create trigger nflow_workflow_update_modified
--     before update on nflow_workflow
--     referencing new as n
--     for each row
--     set modified = current timestamp;

alter table nflow_archive_workflow add priority smallint null;

create index nflow_workflow_polling on nflow_workflow(next_activation, status, executor_id, executor_group);
