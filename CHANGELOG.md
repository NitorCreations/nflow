## next version

**Details**
- nflow-engine:
  - Use more optimal SQL when polling workflows when database supports update returning syntax
  - Only rollback poll operation when no workflows could be allocated for executing (when multiple pollers compete for same workflows)
  - Allow configuring executor queue length with _nflow.dispatcher.executor.queue.size_
  - nflow.transition.delay.waiterror.ms parameter was splitted to nflow.transition.delay.error.min.ms and nflow.transition.delay.error.max.ms
  - Add field `nflow_instance_action.type` that contains type of action:
    - *stateExecution* - for actions created with normal execution. This is also set for old actions created before this feature.
    - *stateExecutionFailed* - for actions where execution failed due thrown exception or retry count was exceeded.
    - *externalChange* - for changes created externally via API.
    - *recovery* - to indicate that the workflow instance was recovered after some executor died.
  - Use more optimal SQL when updating workflows when database supports updateable cte syntax
- nflow-rest:
  - Add support for user-provided action description when updating a workflow instance
  - added missing configuration options with default values
  - Add support for Action.type
- nflow-jetty:
  - added missing configuration options with default values

## 1.2.0 (2014-12-23)

**Highlights**
- nFlow explorer (user interface) beta-version added
- workflow definitions are persisted, and persisted undeployed definitions are returned by REST API
- services for querying executor group and workflow definition statistics

**Details**
- nflow-engine:
  - workflow initial state must be of type "start"
  - workflow error state must be of final ("end" or "manual")
  - workflow instance cannot be externally updated (WorkflowInstanceService.updateWorkflowInstance), when being processed by nFlow executor
  - persists workflow definitions to database during startup (nflow_workflow_definition-table required)
  - internal components annotated by @NFlow (required e.g. for injecting application datasource to nflow)
  - does not start anymore, if transactions are not enabled
  - defining contradictory failure transitions using permit() no longer allowed
  - bug fixes:
    - theoretical problem in optimistic locking of instance polling
    - binary backoff integer overflow when calculating next activation after 15 retries
- nflow-rest-api:
  - return http code 404, when the requested object is not found
  - /v1/workflow-definition (GET)
    - returns persisted workflow definition, if the definition exists (or has existed) within the executor group, but is not deployed to the queried nFlow installation
  - /v1/workflow-definition/{type}/statistics (GET)
    - new resource for retrieving counts of workflow instances in different states
  - /v1/workflow-instance, /v1/workflow-instance/{type} (GET)
    - returns new workflow instance fields: started, created, modified, retries and actions[n].executor_id
    - added query parameter limit search result size (maxResults)
  - /v1/workflow-instance (PUT)
    - returns http code 409, if the instance is currently being processed by nFlow executor
    - bug fix: updating only next activation without state change works now
  - /v1/statistics (GET)
    - new resource for retrieving workflow instance queue size and lag from target activation times
- nflow-jetty:
  - added nflow-explorer user interface beta-version
    - search, manage and visualize workflow instances
    - visualize workflow definitions
    - visualize and monitor instance statistics per definition
  - Swagger runtime dependency removed: service descriptions are generated in compile time to nflow-rest-api -module and served by nflow-jetty
  - tuned web contexts
    - /api/v1/: nFlow REST API version 1
    - /doc/: nFlow REST API Swagger descriptions
    - /explorer/: nflow-explorer user interface

## 1.1.0 (2014-10-16)
- nflow-engine
  - added executor_id to table nflow_workflow_action and exposed executor_ids in WorkflowInstanceService
  - exposed workflow executors through WorkflowExecutorService
  - state handling logic changes:
    - stop in non-final state puts workflow to error state
    - process error state handler method after exceeding max retries, unless workflow instance was already in error state
    - fix: obey maxRetries when NextAction.retryAfter(...) is used
    - final state handler methods should return void instead of NextAction (in 2.0.0 returning NextAction will throw exception)
  - expose new fields through StateExecution: workflow instance id, workflow instance external id
  - insert WorkflowInstanceAction for each recovered WorkflowInstance
  - deprecated (removed in 2.0.0):
    - WorkflowState.getName() (use name() instead)
    - workflow instance "owner" (new term: "executor group")
    - WorkflowInstanceAction.workflowId (new field: workflowInstanceId)
  - internal:
    - renamed WorkflowExecutor->WorkflowStateProcessor, WorkflowExecutorFactory->WorkflowStateProcessorFactory
    - use configured value for workflow dispatcher awaitTermination
- nflow-rest-api
  - added filter for adding CORS headers
  - exposed state variables in REST API
  - exposed workflow executors through REST API
  - store requestData under key "requestData" (previously: "req")
- nflow-jetty
  - enabled transactions using DataSourceTransactionManager
  - swagger-tuning:
    - nFlow branding (titles, favicons, links, etc)
    - configure basepath through swagger.basepath.* -properties

## 1.0.0 (2014-09-13)
- nflow-engine
  - New API between nflow-engine and workflow implementations (StateExecution --> NextAction)
  - Added JavaDoc to public API
  - Support for multiple start states
  - Disallow overloading state methods in WorkflowDefinitions
  - Add executor_group to nflow_workflow_uniq index
  - Change nflow_workflow.external_id to not null in database
  - Use binary backoff for errors by default. Add builder for WorkflowSettings.
  - Set nflow_workflow.state_text only when retrying
  - Rename instantiateNull to instantiateIfNotExists (@StateVar annotation)
  - Check that busy loop next activation is never before now plus small activation delay
- nflow-rest-api
  - REST API from v0 to v1 context
  - Add externalId to ListWorkflowInstanceResponse
- nflow-tests
  - Use fail-fast rule for integration tests
- Code cleanup
  - Changed bean names to camelCase
  - Replace System.currentTimeMillis with DateTimeUtils.currentTimeMillis

## 0.3.1 (2014-08-19)
- Do not log exception, when "Race condition in polling workflow instances detected" happens
- Make dispatcher wait "random(0,1) * short wait time" after race condition (so that probability for race condition lowers in the next poll)
- Sort workflow instances by id before trying to reserve them in dispatcher (otherwise deadlocks may occur)
- Removed pollNextWorkflowInstanceIds from nflow-engine public API

## 0.3.0 (2014-08-14)
- Spring 3.2.x compatibility (previously only 4.0.x)
- Divided nflow-engine API to internal and public java packages
- Added 'executor group' concept: nFlow engines update heartbeat in database; workflow instances reserved for dead engines are auto-recovered
- integration to metrics library http://metrics.codahale.com/
- Starting nFlow engine through Spring lifecycle listener
- Allow custom ThreadFactory for creating nFlow threads
- Handle request data attached to workflow instances as state variable
- Timestamps (created, modified, etc) always set by database
- Splitted RepositoryService into WorkflowInstanceService and WorkflowDefinitionService
- Service for waking up workflow instances
- REST API: always return timestamps in ISO-8601 format
- Use 'official' JodaModule for DateTime (de)serialization
- Workflow instance idempotency management moved from nflow-rest to nflow-engine
- Support for legacy MySql databases (5.5 or below)
- Support for MariaDB 5.5.5
- Coveralls - GitHub integration
- Manual and end states no longer require handler methods; unexistent handler method causes workflow instance to be descheduled
- If no next state defined, workflow instance is put to error state
- Bug fix: state variables were saved multiple times, if the next workflow step was immediately executed

## 0.2.0 (2014-06-14)
- Choose database through Spring profile (nflow.db.h2, nflow.db.mysql, nflow.db.postgresql)
- Improved workflow instance dispatcher
- Improved integration test framework (nflow-tests)
- Increased unit test coverage
- Travis CI build against all supported databases for both openjdk7 and oraclejdk8
- Custom serialization through JacksonJodaModule instead of NflowJacksonObjectMapper
- Support for MySql 5.5.x

## 0.1.0 (2014-05-30)
- Initial public version
