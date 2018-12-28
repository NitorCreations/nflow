## 5.3.0 (yyyy-MM-dd)

**Highlights**
- Add experimental DB2 support

**Breaking changes**
- nFlow `Datasource` uses now underlying database specific `Driver`s instead of `DataSource`s.
  Make a corresponding change, if you have customized `nflow.db.*.driver` parameters.

**Details**
- Upgraded Spring to version 5.1.3.RELEASE
- Workflow instance history (actions, states) cleanup as part of state processing, configurable through `WorkflowSettings`

## 5.2.0 (2018-11-20)

**Highlights**
- Add `stopped` field to executor data. In database, REST API and nFlow Explorer. Requires database migration.
- Support Azure AD authentication in Explorer (see `nflow-explorer/src/config.js` for configuration options)

**Details**
- Travis CI no longer runs tests with Oracle JDK 10. Only oraclejdk8 and openjdk11 are currently running Travis CI tests.
- Upgraded Spring to version 5.1.2.RELEASE
- Explorer displays a link in the header based on `returnUrl` and `returnUrlLabel` parameters in the Explorer opening URL
- nflow-netty's StartNflow interface changed to match nflow-jetty's
- Automatic refresh for workflow instance page in Explorer
- Downgrade org.reclections:reflections to 0.9.10 due to excessive logging in nflow-jetty startup when using 0.9.11

## 5.1.0 (2018-10-18)

**Highlights**
- Multiple bug fixes to nFlow Explorer, including Google Chrome crash fix
- Merge nFlow Explorer to nFlow repository

**Details**
- `nflow-engine`
  - Allow non-final error states in workflow definitions
- nFlow Explorer
  - Fix Google Chrome crash on workflow graph visualization (e.g. switching between parent and child workflow instances crashed Chrome)
  - Fix interaction between selected workflow graph node, action history row and manage state selection
  - Fix execution phases -graph in radiator of workflow definition
  - Bar chart in active instances of workflow definition: vertical to horizontal orientation, removed filters and formatting options
  - Enable pan and zoom in workflow graphs
- Run tests in Travis CI also with OpenJDK 11
- Upgraded Spring to version 5.1.0.RELEASE

## 5.0.1 (2018-09-14)

**Highlights**
- Use [nFlow Explorer version 1.2.8](https://github.com/NitorCreations/nflow-explorer/releases/tag/v1.2.8)

## 5.0.0 (2018-09-11)

**Highlights**
- Fix to work with Spring Boot 2.x and Spring 5.x
- Support for MS SQL database
- Support REST API based on spring-web in addition to JAX-RS.
- Experimental support for the [Netty server](https://netty.io/)

**Breaking changes**
- Renamed JAX-RS REST API project from `nflow-rest-api` to `nflow-rest-api-jax-rs`.
  - By default, the JAX-RS paths were changed from `/nflow/v1/*` to `/v1/*`.
- `nflow-jetty` now serves all paths under `/nflow/*`. The new paths are as follows:
  - /nflow/api/v1           -> API v1 (was: /api/nflow/v1)
  - /nflow/api/swagger.json -> Swagger config (was: /api/swagger.json)
  - /nflow/ui               -> nFlow statics assets 
  - /nflow/ui/explorer      -> nFlow UI (was: /explorer)
  - /nflow/ui/doc           -> Swagger UI (was: /doc)
  - /nflow/metrics          -> metrics and health checks (was: /metrics)
- Removed the following `nflow-jetty` configuration properties:
  - `nflow.swagger.basepath`
  - `nflow.api.basepath`
- `nflow-jetty` Swagger UI no longer by default searches the entire classpath for Swagger annotations.
  - The behaviour can be controlled via the parameter `nflow.swagger.packages`.

**Details**
- Split nflow-rest-api into `nflow-rest-api-jax-rs` and `nflow-rest-api-spring-web` to reflect the targeted framework
- Add configuration parameter `nflow.autoinit` (defaults to true) that can be used to prevent database access on Spring inialization.
- `nflow-rest-api-jax-rs`:
  - Change endpoint paths for workflow instance: /v1/workflow-instance/{id} -> /v1/workflow-instance/id/{id}
- `nflow-server-common`:
  - Use nFlow Explorer version 1.2.7 (nFlow REST API path changes)
- Experimental new module `nflow-netty`
  - Based on [Spring WebFlux](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux) and Netty.
  - Serves a limited subset of `nflow-jetty` paths with REST API under `/nflow/api/v1` and nflow explorer under `/nflow/ui/explorer`.
- `nflow-jetty`
  - Added configuration parameter `nflow.swagger.packages` (defaults to `io.nflow.rest`), that controls which packages are searched for Swagger annotations.
  - Removed parameters `nflow.swagger.basepath` and `nflow.api.basepath` as everything is now prefixed under `/nflow/*`, which should remove the need to control the basepaths.

## 4.2.0 (2017-05-16)

**Highlights**
- Set state variables when inserting or updating workflow instance via REST API
- Control which properties are loaded when getting workflow instance with WorkflowInstanceService

**Details**
- nflow-engine:
  - Control which properties are loaded when getting workflow instance with WorkflowInstanceService: workflow started timestamp, child workflow identifiers, current state variables, actions and action state variables.
- nflow-jetty:
  - Use nFlow Explorer version 1.2.5 (support for setting workflow instance state variables)
  - Fix https://github.com/NitorCreations/nflow/issues/212: make JAXRS server address configurable
- nflow-rest-api:
  - Set state variables when inserting or updating workflow instance

## 4.1.0 (2017-03-31)

**Highlights**
- Support workflow instance signals (see details below)
- Make MAX_SUBSEQUENT_STATE_EXECUTIONS configurable per workflow definition and state
- Workflow instance builder now supports putting state variables with optional value
- Add typed getStateVariable methods to WorkflowInstance (similar to StateExecution.getStateVariable methods)
- Add method to get the (optional) parent workflow instance id to StateExecution
- Fix https://github.com/NitorCreations/nflow/issues/217

**Details**
- nflow-engine:
  - Support for getting and setting workflow instance signal value. Signals may be used by workflow state implementations, for example for interrupting long running workflow state executions. Workflow definitions may document supported signal values. When an unsupported signal value is set, a warning is logged.
- nflow-jetty:
  - Use nFlow Explorer version 1.2.4 (support for workflow instance signals)
- nflow-rest-api:
  - Support for getting and setting workflow instance signal value
  - Support for getting supported signal values of workflow definition
  - When getting workflow actions, return empty array instead of null when instance has no actions

## 4.0.0 (2016-10-28)

**Highlights**
- Rename com.nitorcreations.nflow package to io.nflow
- Require Java 8 (e.g. Java 7 is not supported anymore)
- Wake up parent workflow only if it is in expected state

**Details**
- nflow-engine:
  - Add human-readable toString method to all DTO classes
  - When waking up parent workflow, expected states can be defined. If the state is not as expected, parent workflow is not woken up.
- nflow-jetty:
  - Use nFlow Explorer version 1.2.3

## 3.3.0 (2016-05-27)

**Highlights**
- Fixes to nflow-rest-api CORS header filter
- Fixes to query workflow instances service

**Details**
- nflow-engine:
  - Filter query workflow instances result by executor group
  - Query workflow instances returns latest instances first
- nflow-rest-api:
  - CorsHeaderContainerResponseFilter is applied to nFlow REST API resources only
  - CorsHeaderContainerResponseFilter can be disabled via configuration (nflow.rest.cors.enabled=false)
  - Allowed CORS headers can be configured by setting nflow.rest.allow.headers
- nflow-jetty:
  - Use nFlow Explorer version 1.2.1

## 3.2.0 (2016-04-29)

**Highlights**
- Support for not saving an action when workflow instance is processed
- Support for limiting the number of actions returned when getting workflow instance via REST API
- Add StateExecution.getVariable(name, type, default) method

**Details**
- nflow-engine:
  - Support for not saving an action when workflow instance is processed by calling StateExecution.setCreateAction(false)
  - Add StateExecution.getVariable(name, type, default) method. Also change StateExecution.getVariable(name, type) so that it returns null when variable is not set instead of throwing a NullPointerException.
  - Adding a new workflow instance wakes the dispatcher thread if it is sleeping and the queue is not full
  - Set nflow_executor.active and nflow_executor_expires when inserting new executor to database, do not accept null values for these columns anymore
- nflow-rest-api:
  - Support for limiting the number of actions returned when getting workflow instance via REST API. The latest actions are returned first.
- Other:
  - Fix or suppress all FindBugs warnings

## 3.1.1 (2016-04-08)

**Details**
- nflow-engine:
  - Do not block shutdown when marking node as not running fails
  - Use batch updates when Oracle database server version is 12.1 or newer
  - Fix automatic database creation to work with Oracle database

## 3.1.0 (2016-04-01)

**Highlights**
- Log stack traces of executors that may be stuck
- Oracle fixes

**Details**
- nflow-engine:
  - Log stack traces for executor threads that may be stuck (nflow.executor.stuckThreadThreshold.seconds)
  - Mark node not running on graceful shutdown
  - Do not use batch updates when using Oracle database
  - Do not set nflow.db.username and nflow.db.password properties in nflow-engine.properties because they override database type specific properties
  - Lower log levels of some log entries that are not that important
- nflow-jetty:
  - Use nFlow Explorer version 1.2.0
- source:
  - Add package level javadocs

## 3.0.0 (2016-02-26)

**Highlights**
- Easier to embed nflow-rest-api to applications
- Production use support features for nflow-jetty

**Details**
- nflow-engine:
  - Independent workflows can be created in state methods (StateExecution.addWorkflows())
  - Added service for checking database connection status (HealthCheckService)
  - Support for MySQL database 5.7.x
  - fixed: workflow instance recovery functionality (broken by version 2.0.0)
  - fixed: Oracle database schema
- nflow-rest-api:
  - **_breaking change:_** Prefixed operation paths by "/nflow" (e.g. /v1/statistics -> /nflow/v1/statistics) 
  - Support for Jersey JAX-RS implementation
  - **_breaking change:_** Moved exception mappers to nflow-jetty (BadRequestExceptionMapper, CustomValidationExceptionMapper, NotFoundExceptionMapper)
  - Improved Swagger documentation
- nflow-jetty:
  - New configuration properties:
    - nflow.jetty.accesslog.directory: access log directory (default "log")
    - nflow.swagger.basepath: Swagger basepath for services (default "/api")
    - nflow.external.config: location for external configuration (default undefined)
  - Added metrics and health check endpoints from [Dropwizard](http://metrics.dropwizard.io/) to /metrics -context
- nflow-metrics:
  - **_breaking change:_** No longer defines MetricRegistry bean (must be injected from outside)
  - Added database check feature for health checks (DatabaseConnectionHealthCheck)

## 2.0.0 (2015-11-06)

**Highlights**
- Support for child workflows
- WorkflowExecutorListeners are now implemened as a listener chain. The listeners may now skip the actual execution of a state method. Useful e.g. for locking.
- Support for archiving old, finished workflow instances

**Details**
- Increase nflow_executor.host length to 253 characters in database. It can now contain a full DNS name.
- When a database is created, nFlow writes a log message about it
- Experimental support for Oracle database
- Added new Maven module for performance tests
- Added WorkflowInstanceAction.id
- Final state methods cannot return value anymore
- Remove deprecated fields: WorkflowInstanceAction.workflowId, WorkflowInstanceService.updateWorkflowInstanceAfterExecution, WorkflowInstance.processing, WorkflowInstance.owner, WorkflowState.getName()
- Small bug fixes and enhancements

## 1.3.0 (2015-04-14)

**Highlights**
- Introduced workflow instance status, which indicates the execution state (created, inProgress, executing, paused, stopped, finished, manual) of workflow instance
- Added REST API services for pausing, resuming and stopping workflow instances
- Restructured workflow definition statistics to return workflow instance status counts for workflow instance states

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
  - Automatically abbreviate state text for workflow instance and workflow instance action based on field size in database
  - Added WorkflowInstance.status (created, in_progress, executing, manual, finished, paused, stopped) for workflow instances
  - Removed WorkflowInstance.processing which is now replaced by WorkflowInstance.status
  - Workflow instance state text is now always set to a meaningful value when the instance is updated
  - Make most configuration properties required and remove the default values from the source code
  - Added missing default values to the configuration files
  - Improved workflow definition statistics query performance and results
  - Retry processing of a workflow instance that has unknown state after some delay (one hour by default, configurable)
  - Move workflow instance to error state when a state processing method returns an invalid next state
- nflow-rest-api:
  - Added support for user-provided action description when updating a workflow instance
  - Added missing configuration options with default values
  - Added support for Action.type
  - Added service to pause, resume and stop the execution of the workflow instance
  - Make most configuration properties required and remove the default values from the source code
  - Added missing default values to the configuration files
  - Moved workflow definitions statistics to /statistics/workflow/{workflow-definition-type} and return statistics per workflow state and status
- nflow-jetty:
  - added missing configuration options with default values
  - Make most configuration properties required and remove the default values from the source code
  - Added missing default values to the configuration files
  - Use nFlow Explorer version 0.0.7

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
