alter table nflow_workflow add started timestamptz;
alter table nflow_archive_workflow add started timestamptz;

update nflow_workflow w
  set started = a.started, modified = w.modified
  from (select workflow_id, min(execution_start) as started from nflow_workflow_action group by workflow_id) a
  where w.id = a.workflow_id;

update nflow_archive_workflow w
  set started = a.started, modified = w.modified
  from (select workflow_id, min(execution_start) as started from nflow_archive_workflow_action group by workflow_id) a
  where w.id = a.workflow_id;
