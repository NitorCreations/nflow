alter table nflow_workflow add started timestamp(3) null;
alter table nflow_archive_workflow add started timestamp(3) null;

update nflow_workflow w, (select workflow_id, min(execution_start) as started from nflow_workflow_action group by workflow_id) a
  set w.started = a.started, w.modified = w.modified where w.id = a.workflow_id;
update nflow_archive_workflow w, (select workflow_id, min(execution_start) as started from nflow_archive_workflow_action group by workflow_id) a
  set w.started = a.started, w.modified = w.modified where w.id = a.workflow_id;
