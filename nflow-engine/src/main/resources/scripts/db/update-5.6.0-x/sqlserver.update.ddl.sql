alter table nflow_workflow add started datetimeoffset(3);
alter table nflow_archive_workflow add started datetimeoffset(3);

go

update w set
  w.started = a.started,
  w.modified = w.modified
from
  nflow_workflow w,
  (select workflow_id, min(execution_start) as started from nflow_workflow_action group by workflow_id) a
where w.id = a.workflow_id;

update w set
  w.started = a.started,
  w.modified = w.modified
from
  nflow_archive_workflow w,
  (select workflow_id, min(execution_start) as started from nflow_archive_workflow_action group by workflow_id) a
where w.id = a.workflow_id;
