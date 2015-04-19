# nFlow Design and Architecture Decision Log

(newest first)

2015-04-19 efonsell
---------------------------
Define default property-values in properties-files only and make them required when there is a static default value. Do not define default values in properties-files for properties that have a dynamic default value. For example, nflow.dispatcher.sleep.ms default value 1000 is defined in nflow-engine.properties, but nflow.executor.thread.count default value which is based on number of processors is defined in the Java code.


2015-01-10 gmokki, efonsell
---------------------------
When polling for next workflow instances in WorkflowInstanceDao, the modified field in OptimisticLockKey is handled as String instead of Timestamp to avoid problems caused by losing millisecond precision from timestamps in some cases (for example with some older versions of MySQL).


2014-12-10 eputtone
-------------------
Internal nFlow functionalities can access DAO layer (package com.nitorcreations.nflow.engine.internal.dao) directly instead of going through service layer (package com.nitorcreations.nflow.engine.service). Rationale: service layer is currently part of public API that we wish to keep as simple as possible. Example: WorkflowDefinitionResource in nflow-rest-api uses WorkflowDefinitionDao directly for retrieving StoredWorkflowDefinitions, because we don't want to confuse public API users with multiple workflow definition representations.


2014-11-26 jsyrjala
-------------------
WorkflowInstance.started value (time when processing of the workflow is first started) is fetched from the earliest nflow_workflow_action.start_execution value. This way no changes to database is needed. If needed because of performance, the value may be later added as nflow_workflow.started. 

Started value is fetched with a subselect. This requires that nflow_workflow_action.workflow_id is indexed. Postgres has explicit index, h2 and mysql have implicit index via foreign key.


2014-11-26 efonsell
-------------------
Swagger-UI static resources are downloaded from GitHub based on version defined in pom.xml and extracted to nflow-jetty target directory, except index.html which is customized and thus updated manually in nFlow repository.
