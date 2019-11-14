alter table nflow_workflow add started timestamp;
alter table nflow_archive_workflow add started timestamp;

update nflow_workflow w set
  started=(select min(execution_start) from nflow_workflow_action a where a.workflow_id = w.id group by a.workflow_id),
  modified=w.modified;

update nflow_archive_workflow w set
  started=(select min(execution_start) from nflow_archive_workflow_action a where a.workflow_id = w.id group by a.workflow_id),
  modified=w.modified;
