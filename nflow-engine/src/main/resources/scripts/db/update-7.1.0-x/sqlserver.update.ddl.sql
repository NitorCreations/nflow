drop index nflow_archive_workflow_uniq;

create index idx_workflow_archive_type on nflow_archive_workflow(type);

drop index nflow_archive_workflow_parent;

CREATE INDEX WITH (DROP_EXISTING = ON, ONLINE = ON) idx_workflow_parent on nflow_workflow(parent_workflow_id) where parent_workflow_id is not null;

CREATE INDEX WITH (DROP_EXISTING = ON, ONLINE = ON) idx_workflow_archive_parent on nflow_archive_workflow (parent_workflow_id) where parent_workflow_id is not null;
