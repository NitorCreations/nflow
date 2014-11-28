# nFlow Design and Architecture Decision Log

(newest first)

2014-11-26 jsyrjala
-------------------
WorkflowInstance.started value (time when processing of the workflow is first started) is fetched from the earliest nflow_workflow_action.start_execution value. This way no changes to database is needed. If needed because of performance, the value may be later added as nflow_workflow.started. 

Started value is fetched with a subselect. This requires that nflow_workflow_action.workflow_id is indexed. Postgres has explicit index, h2 and mysql have implicit index via foreign key.


2014-11-26 efonsell
-------------------
Swagger-UI static resources are downloaded from GitHub based on version defined in pom.xml and extracted to nflow-jetty target directory, except index.html which is customized and thus updated manually in nFlow repository.
