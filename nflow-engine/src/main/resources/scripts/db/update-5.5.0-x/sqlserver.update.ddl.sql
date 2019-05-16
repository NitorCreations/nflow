alter table nflow_workflow add started datetimeoffset(3);
alter table nflow_archive_workflow add started datetimeoffset(3);

update nflow_workflow w, (select workflow_id, min(execution_start) as started from nflow_workflow_action group by workflow_id) a
  set w.started = a.started where w.id = a.workflow_id;
update nflow_archive_workflow w, (select workflow_id, min(execution_start) as started from nflow_archive_workflow_action group by workflow_id) a
  set w.started = a.started where w.id = a.workflow_id;
