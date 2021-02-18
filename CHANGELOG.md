## 7.2.3-SNAPSHOT (future release)

**Highlights**
- Support updating workflow instance business key.
- Support for searching workflow instances by state variable key and value.
- Added optional properties for tuning parts of nFlow Explorer
- Facelift for workflow instance properties in nFlow Explorer
- Control retryable exception handling via `WorkflowSettings` (replaces deprecated `WorkflowState.isRetryAllowed(...)`)
- Control retrying and logging of an exception thrown by a state method via `WorkflowSettings` (replaces deprecated `WorkflowState.isRetryAllowed(...)`)
- Control retrying and logging of an exception thrown by a state method via `WorkflowSettings` (replaces deprecated `WorkflowState.isRetryAllowed(...)`).
- Control logging and sleeping after exceptions in `WorkflowDispatcher`.
- Control logging and sleeping after a failure to save workflow instance state.
- Support in `CronWorkflow` to wait for created child workflow instances to finish before scheduling the next work.

**Details**
- `nflow-engine`
  - Use `WorkflowInstanceService.updateWorkflowInstance` to update the business key of the workflow instance.
  - Use `StateExecution.setBusinessKey` to update the business key of the workflow instance after processing a state.
  - Use `QueryWorkflowInstances.setStateVariable` to limit search query by state variable name and key. Only the latest value of the state variable of the workflow instance is used.
  - Control retrying and logging of an exception thrown by a state method via `WorkflowSettings.Builder.setExceptionAnalyzer(...)` / `ExceptionHandling`:
    - Control whether the exception is considered retryable or not (replaces deprecated `WorkflowState.isRetryAllowed(...)`).
    - Control which log level is used to log the retryable exception.
    - Control whether the stack trace of the retryable exception is logged or not.
  - Control logging and sleeping after exceptions in `WorkflowDispatcher` by providing a `DispatcherExceptionHandler` that returns `DispatcherExceptionHandling` objects:
    - Control whether the exception is logged or not.
    - Control which log level is used to log the exception.
    - Control whether the stack trace of the exception is logged or not.
    - Control whether dispatcher should sleep after the exception or not.
    - Control whether the sleep time should be randomized or not.
  - Control logging after a failure to save workflow instance state by providing a `StateSaveExceptionHandler` that returns `StateSaveExceptionHandling` objects:
    - Control which log level is used to log the exception.
    - Control whether the stack trace of the exception is logged or not.
    - Control how long the `WorkflowStateProcessor` should sleep before retrying.
  - Support in `CronWorkflow` to wait for child workflow instances created in `doWork` state method to finish before scheduling the next work. Return `NextAction.moveToStateAfter(waitForWorkToFinish, ...)` with some fail-safe waiting time instead of `NextAction.moveToState(schedule, ...)` to avoid immediate re-scheduling. When child workflows finish, they will wake up the parent workflow automatically, if it is still in the waiting state. Default implementation will check if any child workflows are still running, and keep waiting until they are all finished. Override `CronWorkflow.waitForWorkToFinishImpl` for custom logic.
  - Add `StateExecution.hasUnfinishedChildren` helper to check if the workflow instance has any child workflow instances with any other status than `WorkflowInstanceStatus.finished`.
- `nflow-rest-api-common`, `nflow-rest-api-jax-rs`, `nflow-rest-api-spring-web`
  - `UpdateWorkflowInstanceRequest.businessKey` field was added to support updating workflow instance business key via REST API.
  - Added support for new query parameters `stateVariableKey` and `stateVariableValue` to `GET /v1/workflow-instance` to limit search query by state variable name and key. Only the latest value of the state variable of the workflow instance is used.
  - Fix REST API serialization of state variable values that are not valid JSON by failing the serialization on trailing tokens and resorting to string representation as expected.
- `nflow-explorer`
  - Added optional `config.js` properties (`htmlTitle`, `nflowLogoFile`, `hideFooter`)
  - Facelift for workflow instance properties
  - Dependency updates:
    - urijs 1.19.5
    - socket.io 2.4.1
- Database
  - Change `text` data types to `varchar(max)` for MS SQL Server

## 7.2.2 (2020-12-25)

**Highlights**
- `nflow-engine`
  - Handle exceptions if waking up parent workflow fails after updating workflow instance state.
- `nflow-explorer`
  - Remove "Save as PNG" button as it produced empty images. PR to restore functionality would be appreciated.

**Details**
- `nflow-engine`
  - Catch and log error when waking up parent workflow fails after updating workflow instance state. Failure in waking up parent workflow should not trigger a retry for the workflow instance update, as the update has already been done. As the parent is expected to handle situations where child workflow does not explicitly wake up the parent, it is ok to just log this error and continue.

## 7.2.1 (2020-12-23)

**Highlights**
- `nflow-engine`
  - Optimize fetching workflow instance with large history with many modified state variables.

- `nflow-rest-api-spring-web` and `nflow-netty`
  - Change REST API calls to use a dedicated thread pool for all blocking database operations to avoid blocking the netty EventLoop thread.

**Details**
- `nflow-engine`
  - Fix SQL performance / memory issue when getting single workflow instance with action state variables, when the instance has lots of actions with lots of state variables. The old code fetched all state variables of all actions of the instance, the new code only fetches the state variables for the actions that will be returned.
  - Fix instantiation of @StateVar(instantiateIfNotExists=true) Mutable<Type> - the result was incorrectly a Mutable wrapped in Mutable.
  - Fix potential resource leaks
  - Dependency updates:
    - spring 5.2.8
    - jackson 2.12.0
    - hikaricp 3.4.5
    - jodatime 2.10.8
    - mysql 8.0.20
    - node 12.16.2
    - netty 0.9.11
    - apache cxf 3.4.1
    - commons-lang3 3.11
    - hibernate validator 6.1.7.Final
    - mariadb 2.7.1
    - metrics 4.1.16
    - mssql 8.4.1.jre8
    - mysql 8.0.22
    - postgresql 42.2.18
    - swagger 1.6.2
    - jetty 9.4.35.v20201120
    - mockito 3.6.28
    - spotbugs 4.2.0
- `nflow-explorer`
  - Dependency updates:
    - autoprefixer 9.7.6
    - node-sass 4.14.1
    - http-proxy 1.18.1
    - ini 1.3.7
    - bl 1.2.3
- nflow-rest-api-spring-web
  - Change deendency from spring-web to spring-webflux to be able to use Project Reactor's types.
  - Introduce a thread pool in SchedulerService and wrap all blocking database calls in the REST API to it.

## 7.2.0 (2020-04-27)

**Highlights**
- Expedited clean shutdown for workflows that run many consequtive states.
- Add support for custom logic when nFlow engine thinks the workflow state processing may be stuck.
- Convert all exceptions to proper error response messages in all nFlow REST API endpoints. Applies to both `nflow-rest-api-jax-rs` and `nflow-rest-api-spring-web`.
- Support for Guice.

**Details**
- `nflow-engine`
  - When shutdown is requested, stop processing workflows immediately after the current state has been executed.
  - Add `WorkflowExecutorLister.handlePotentiallyStuck(Duration processingTime)` to support custom handling when nFlow engine thinks the workflow state processing may be stuck. If any registered listener implementation returns true from this method, nFlow will interrupt the processing thread. The default implementation returns false.
  - Throw `IllegalArgumentException` instead of `IllegalStateException` when trying to update workflow instance state to an invalid value.
  - Throw `IllegalArugmentException` instead of `RuntimeException` when trying to insert workflow instance with unknown type or with a state that is not a start state.
  - Make `StateVariableTooLongException` extend `IllegalArgumentException` instead of `RuntimeException`.
  - Fix SQL deadlocks in workflow instance polling for PostgreSQL with skip locked.
  - Add `EngineEnvironmentModule` and `EngineModule` for Guice support. Call `NflowController.start()` and `NflowController.stop()` to start and stop nFlow engine, as `nflow.autostart` and `nflow.autoinit` configuration options are not supported with Guice.
  - MySQL/MariaDB: use compressed table format for archives.
  - PostgreSQL/SQLServer: use partial (not null) indices for parent hierarchy.
  - PostgreSQL: tune fillfactors of tables and indices.
  - PostgreSQL: reorder table column order to minimize space lost on padding. Only affects new tables.
  - Drop `(type, external_id, executor_group)` unique constraint from `nflow_archive_workflow` table. Allows archiving workflow instances with same values multiple times. Only one instance is allowed in production table though.
  - Dependency updates:
    - spring 5.2.5
    - jackson 2.10.3
    - javassist 3.27.0
    - jetty 9.4.28
    - cxf 3.3.6
    - metrics 4.1.6
    - netty 0.9.6
    - swagger 1.6.1
    - lang3 3.10
    - mariadb 2.6.0
    - mssql 8.2.2
    - postgresql 42.2.10
    - junit 5.6.2
    - mockito 3.3.3
    - spotbugs 4.0.2
    - hibernate 6.1.4
    - commons-lang3 3.10
- `nflow-rest-api-jax-rs` and `nflow-rest-api-spring-web`
  - Convert `IllegalArgumentException` to `HTTP 400 Bad Request` with a valid JSON response body in all endpoints.
  - Convert `NflowNotFoundException` to `HTTP 404 Not Found` with a valid JSON response body in all endpoints.
  - Convert all other throwables to `HTTP 500 Internal Server Error` with a valid JSON response body in all endpoints.
- `nflow-explorer`
  - Dependency updates:
    - swagger-ui 2.2.10
    - es5-shim 4.5.14
    - npm 6.14.4
    - autoprefixer 9.7.5
    - grunt 1.1.0
    - grunt-contrib-imagemin 4.0.0

## 7.1.0 (2020-03-25)

**Highlights**
- `nflow-engine`
  - Add support for marking exceptions and workflow states as non-retryable. If processing the state method throws an exception, nFlow engine will check if it
    is allowed to retry. If not, the workflow instance will go directly to a failure/error state. See `NoRetryWorkflow` for example.

**Details**
- `nflow-engine`
  - Exceptions can be annotated with `@NonRetryable` to prevent retrying the state processing.
  - Add `isRetryable(Throwable thrown)` method to the `WorkflowState` interface with a default implementation that returns true when the thrown exception is not
    annotated with `@NonRetryable`.
  - Add default implementation for `WorkflowState.getDescription()` that returns the name of the state.

## 7.0.0 (2020-03-05)

**BREAKING CHANGES**
  - Drop `nflow_workflow.root_workflow_id` column.
  - Remove, add and update foreign key constraints and indices in nFlow database tables.
  - `ArchiveService` and `ArchiveResource` (`/v1/archive`) are replaced by `MaintenanceService` and `MaintenanceResource` (`/v1/maintenance`).
  - Change `WorkflowSettings.setHistoryDeleteableAfterHours(Integer)` to `WorkflowSettings.setHistoryDeleteableAfter(ReadablePeriod)` for more flexible configuration.
  - `StateExecution.getAllChildWorkflows` does not return current state variables of child workflows anymore.
  - `WorkflowStateProcessor` does not read child workflow IDs of processed workflow instances anymore.
  - `QueryWorkflowInstances.Builder` does not query for current state variables by default anymore.
  - `BulkWorklow` moved to `io.nflow.engine.workflow.curated` java package.

**Highlights**
- `nflow-engine`
  - Improve workflow instance archiving performance. Archiving has been in practice unusable in some scenarios.
  - Add support for deleting workflow instances, actions and state variables from production and archive tables.
  - Remove support for `nflow.executor.fetchChildWorkflowIds` configuration property.
  - Fix bug in stuck workflow instance processing detection when multiple states are processed consecutively by the same processor thread. Now the time counter is reset between each state.
  - Added abstract `CronWorkflow` that can be used to periodically execute a task.
  - Built-in support for periodically running maintenance to clean up workflow instances.

**Details**
- `nflow-engine`
  - `nflow_workflow.root_workflow_id` was only used in old archiving code, so it was removed as the new archiving logic does not need it anymore.
  - Removed unnecessary indices and foreign keys and added missing indices to improve nFlow database performance. See database update scripts for details.
  - Added name for all existing and new constraints in create scripts, if they did not have one yet. This is to make modify operations easier in future. All of these may not be covered in database update scripts.
  - See `MaintenanceService` and `MaintenanceConfiguration` for details on how to archive and delete workflow instances. The maintenance operations can now be limited by workflow type as well.
  - As ArchiveService is removed, the old functionality of `ArchiveService.archiveWorkflows(DateTime olderThan, int batchSize)` can now be achieved with
    `MaintenanceService.cleanupWorkflows(new MaintenanceConfiguration.Builder()
      .setArchiveWorkflows(new ConfigurationItem.Builder()
        .setOlderThanPeriod(new Period(olderThan, DateTime.now())
        .setBatchSize(batchSize)
        .build())
      .build)`
  - For example, `WorkflowSettings.setHistoryDeleteableAfterHours(12)` can now be achieved by `WorkflowSettings.setHistoryDeleteableAfter(Period.hours(12))`.
  - To get child workflows with state variables in a state method, use `StateExecution.queryChildWorkflows`.
  - To get all child workflow IDs in a state method, use `StateExecution.getAllChildWorkflows`.
  - New `MaintenanceWorkflow` that can be enabled by setting `nflow.maintenance.insertWorkflowIfMissing` property to true. On the first run the workflow is inserted and it will start to run periodically. Further configuration must be done by editing the two state variables of the workflow: `cron` for the scheduling and `config` for the `MaintenanceConfiguration` to execute.
    - The first time configuration can also be tuned with `nflow.maintenance.initial.cron`, `nflow.maintenance.initial.delete.olderThan`, `nflow.maintenance.initial.archive.olderThan` and `nflow.maintenance.initial.deleteArchived.olderThan`
  - Dependency updates:
    - spring 5.2.4
    - jersey 2.30.1
    - metrics 4.1.3
    - netty 0.9.5
    - mockito 3.3.0
    - spotbugs 4.0.0
- `nflow-explorer`
  - Dependency updates:
    - angular extra libraries 1.7.9
    - angular-ui-router 1.0.25
    - dagre-d3 0.6.4
    - node-sass 9.7.4
    - karma-jasmine 3.1.1
    - autoprefixer 9.7.4
    - node v12.16.1
    - npm 6.13.7

## 6.2.0 (2020-02-11)

**Highlights**
- `nflow-engine`
  - Improve child workflow final state execution speed by caching the parent workflow type. See configuration details below.
  - Add optional performance improvement to state execution. See configuration details below.
  - Optimize workflow instance polling code.

**Details**
- `nflow-engine`
  - Workflow type cache size can be set using `nflow.db.workflowInstanceType.cacheSize` property. Default is 10000.
  - Set property `nflow.executor.fetchChildWorkflowIds` to `false` (recommended) to avoid reading child workflow IDs when executing a state. If access to child workflow instances is needed, they can be fetched using `StateExecution` interface. This will be the default in 7.0.0 release.
  - Automatically disable batch updates if batch update returns unsupported values.
  - Fetch optimistic locked list of workflow instance IDs in a separate transaction from the modify phase. This should reduce the risk of deadlocks in databases.
  - Tune batch status handling to consider status a failure if more than one row were modified.
- Dependency updates:
  - spotbugs-annotations 4.0.0-RC3
  - mssql 8.2.0.jre8
  - jetty 9.4.26.v20200117
  - mariadb 2.5.4
  - postgresql 42.2.10
  - hibernate-validator 6.1.2.Final
- Maven plugin updates:
  - spotbugs 4.0.0-RC3

## 6.1.0 (2020-01-23)

**Highlights**
- `nflow-engine`
  - Check that state variable value fits into the database column
  - Fix performance problems when querying workflows by only fetching state variables when requested

**Details**
- `nflow-engine`
  - Verify on startup that connections returned by `DataSource` used by nFlow have auto commit enabled. nFlow may not work correctly without it.
  - Throw `StateVariableValueTooLongException` if a state variable value that does not fit into the database column is detected. Checked in `StateExecution.setVariable`, `StateExecution.addWorkflows`, `StateExecution.addChildWorkflows`, `WorkflowInstanceService.insertWorkflowInstance` and when creating a new or updating an existing instance via REST API.
    If the exception is thrown during state processing and not handled by the state implementation, nFlow engine will catch the exception and retry state processing after delay configured by property `nflow.executor.stateVariableValueTooLongRetryDelay.minutes` (default is 60).
  - Fix honoring of `includeCurrentStateVariables` flag in `WorkflowInstanceService.listWorkflowInstances`. This caused major slowness when using bulk workflows.
    To preserve the existing (incorrect) default behaviour in backwards compatible way the default value in `QueryWorkflowInstances.Builder` is changed to `true`. The REST API is unaffected.
    Especially in workflows with many children that use the `StateExecution.getAllChildWorkflows` method the performance impact can be high. Before 7.0.0 release, it is recommended to use `StateExecution.queryChildWorkflows(new QueryWorkflowInstances.Builder().setIncludeCurrentStateVariables(false).build())` if state variables are not needed.
  - Dependency updates:
    - jetty 9.4.25.v20191220
    - junit4 4.13
    - mariadb 2.5.3
    - spotbugs 4.0.0-beta4
    - spotbugs-annotations 4.0.0-beta4
    - swagger 1.6.0
    - hikaricp 3.4.2
    - jackson 2.10.2
    - jersey 2.30
    - mockitor-junit-jupiter 3.2.4
    - metrics 4.1.2
    - mockito 3.2.4
    - mysql 8.0.19
    - postgresql 42.2.9
    - netty 0.9.4.RELEASE
    - reflections 0.9.12
    - spring 5.2.3.RELEASE
    - junit5 5.6.0
    - apache-cxf 3.3.5
- `nflow-rest-api`
  - REST API returns HTTP 400 in case the state variable value is too long when inserting a new or updating an existing workflow instance
  - Exception mappers return JSON objects instead of raw strings
- `nflow-explorer`
  - Dependency updates:
    - angular 1.7.9
    - chart.js 2.9.3
    - karma 4.4.1
    - node-sass 4.13.0
    - node v12.13.1
    - npm 6.13.1
- Maven plugins
  - Dependency updates:
    - frontend 1.9.0
    - assembly 3.2.0
    - asciidoctor 2.0.0-RC.1
    - deploy 3.0.0-M1
    - enforcer 3.0.0-M3
    - install 3.0.0-M1
    - jar 3.2.0
    - site 3.8.2
    - source 3.2.1
    - surefire 3.0.0-M4

## 6.0.0 (2019-11-26)

**Highlights**
- Add priority to workflow instances
- Use constructor injection instead of field or setter injection in nFlow classes
- Separate workflow definition scanning from `WorkflowDefinitionService`
- Remove deprecated `WorkflowInstanceInclude.STARTED` enum value
- Remove deprecated `AbstractWorkflowExecutorListener`, use `WorkflowExecutorListener` instead
- Remove deprecated `WorkflowInstance.setStarted`, use `WorkflowInstance.setStartedIfNotSet` instead
- Add MariaDB support
- Add Kotlin example using nFlow with Spring Boot, and integrated nFlow Explorer
- Expose wakeup via REST-API
- Update database indices to match workflow instance polling code
- Add new configuration option `nflow.db.disable_batch_updates` to work around some Galera oddities

**Details**
- `nflow-engine`
  - Add `priority` two byte integer to the `nflow_workflow` table. When the dispatcher chooses from many available scheduled workflow instances it primarily (unfairly) picks the workflow instances with the largest priority values, and for workflows with the same priority, the ones with oldest `next_activation` timestamp. Priority defaults to 0 and can also be negative. Default priority value for the new workflow instances can be set per workflow definition (`WorkflowSettings.Builder.setDefaultPriority`), and overridden per workflow instance (`WorkflowInstance.Builder.setPriority`). Requires database migration, see database update scripts for details.
  - Separate workflow definition scanning from `WorkflowDefinitionService` by introducing `WorkflowDefinitionSpringBeanScanner` and `WorkflowDefinitionClassNameScanner`. This allows breaking the circular dependency when a workflow definition uses `WorkflowInstanceService` (which depends on `WorkflowDefinitionService`, which depended on all workflow definitions). This enabled using constructor injection in all nFlow classes.
  - Add `disableMariaDbDriver` to default MySQL JDBC URL so that in case there are both MySQL and MariaDB JDBC drivers in the classpath then MariaDB will not steal the MySQL URL.
  - Add support for `nflow.db.mariadb` profile.
  - Update database indices to match current workflow instance polling code.
  - Create indices for foreign keys in MS SQL database.
  - Fix create database scripts to work with empty database.
  - Add `nflow.db.disable_batch_updates` (default `false`) configuration parameter to make it possible to force use of multiple updates even if batch updates are supported by the database. This is needed on some Galera systems that cannot handle batch updates correctly.
  - Dependency updates:
    - reactor.netty 0.9.1.RELEASE
    - jackson 2.10.1
    - mysql-connector-java 8.0.18
    - mariadb jdbc 2.4.4
    - postgresql jdbc 42.2.8
    - mssql-jdbc 7.4.1.jre8
    - metrics 4.1.1
    - junit5 5.5.2
    - hikaricp 3.4.1
    - jetty 9.4.20.v20190813
    - apache-cxf 3.3.4
    - slf4j 1.7.29
    - spring 5.2.1.RELEASE
    - hibernate-validator 6.1.0.Final
    - joda-time 2.10.5
    - swagger 1.5.24
    - mockito 3.1.0
    - hamcrest 2.2
    - h2 1.4.200
    - javassist 3.26.0-GA
    - jetty 9.4.21.v20190926
- `nflow-explorer`
  - Dependency updates
    - nodejs 10.16.3
    - npm 6.11.3
    - lodash 4.7.15
- `nflow-examples`
  - Update Spring Boot examples' dependencies
    - Spring Boot 2.1.7.RELEASE
    - nFlow 5.7.0
    - Gradle 5.5.1
- New REST-API endpoint to wake up workflow instance sleeping in specified states:
  PUT /nflow/api/v1/workflow-instance/5/wakeup
- Improve error logging in WorkflowStateProcessor.
- Replace FindBugs with SpotBugs.
- Drop index from main table that was only used for archiving purposes.
- Increase workflow instance and action identifiers in code to 64 bits for future proofing. Database schema is not changed for now, but the columns can be altered later (to bigints) if needed.

## 5.7.0 (2019-06-06)

**Highlights**
- Added `started` timestamp to workflow instance table (requires database update)
- Deprecated WorkflowInstanceInclude.STARTED enum value
- Deprecated `AbstractWorkflowExecutorListener`, use `WorkflowExecutorListener` instead
- Allow easily starting nflow-engine for embedding via `NflowEngine` class
- Added `WorkflowLogContextListener` for setting generic workflow properties to SLF4J log context and logging state variables when processing any state of any workflow instance

**Details**
- `nflow-engine`
  - Add started timestamp to workflow instance table. This makes the instance queries much faster when instances have lots of actions, as there is no need to join the nflow_workflow_action table to the query anymore.
  - Deprecated WorkflowInstanceInclude.STARTED enum value. This is not needed anymore, since the started timestamp is always read from the database when the instance is loaded.
  - Moved default implementations for `WorkflowExecutorListener` interface methods from the abstract class to the interface.
  - Fixed bug with dropping non-existent index in PostgreSQL create script.
- Dependency updates:
   - reactor.netty 0.8.8.RELEASE
   - jetty 9.4.18.v20190429
   - javassist 3.25.0-GA
   - mysql-connector-java 8.0.16
   - mssql-jdbc 7.2.2.jre8
   - metrics 4.1.0
   - spring 5.1.7.RELEASE
   - hibernate.validator 6.0.15.Final
   - cxf 3.3.2
   - joda-time 2.10.2
   - commons-lang3 3.9
   - jackson 2.9.9
   - junit 5.4.1
   - mockito 2.27.0
- `nflow-explorer`
  - Dependency updates

## 5.6.0 (2019-05-21)

**Highlights**
- Support non-enum WorkflowStates to enable, for example, Kotlin workflow definitions by extending AbstractWorkflowDefinition.

**Details**
- Dependency and plugin updates:
  - spring 5.1.6.RELEASE
  - reactor.netty 0.8.6.RELEASE
  - jetty 9.4.17.v20190418
- `nflow-engine`
  - Retry workflow state processing until all steps in nFlow-side are executed successfully. This will prevent workflow instances from being locked in `executing` status, if e.g. database connection fails after locking the instance and before querying the full workflow instance information (`WorkflowStateProcessor`).
  - Fix #306: create empty ArrayList with default initial size.
  - Log more executor details on startup.
  - Fix #311: Replace references to WorkflowDefinition with AbstractWorkflowDefinition to support non-enum WorkflowStates
  - Use name() instead of toString() when getting workflow instance initial state name.

## 5.5.0 (2019-04-04)

**CRITICAL BUG**
This release contains a bug (#306, introduced in 5.4.1) which may cause OutOfMemory errors while fetching child workflow IDs. We recommend to update to 5.6.0 as soon as possible.

**Highlights**
- Introduce possibility to temporarily stop polling for new workflow instances by invoking pause() on WorkflowLifecycle, continue polling with resume(), and check pause status with isPaused().

**Details**
- Update libraries for nFlow Explorer. Includes fix for morgan library security issue.
  - https://github.com/NitorCreations/nflow/network/alert/nflow-explorer/package-lock.json/morgan/open
- Fix travis build to actually run unit tests for nflow-explorer module.
- Add pause(), resume() and isPaused() to WorkflowLifecycle, to pause and resume workflow instance polling in a running nFlow.

## 5.4.1 (2019-03-18)

**CRITICAL BUG**
This release introduced issue #306 which may cause OutOfMemory errors while fetching child workflow IDs. We recommend to update to 5.6.0 as soon as possible.

**Highlights**
- Introduce BulkWorkflow which can be used or extended to handle mass of child workflows without overloading the system.
- Introduce new workflow instance state type `wait`. Child workflow instances automatically wake up the parent when the parent is in a `wait` state and the child enters an `end` state.
- Allow creating workflows via REST API with null activation time (by setting `activate = false`).
- Allow creating child workflows via REST API (by setting `parentWorkflowId`).

**Details**
- See `BulkWorkflowTest` and `DemoBulkWorkflow` for examples on how to use bulk workflows
- Support boxed primitives (Integer, Float etc) with @StateVar
- nFlow Explorer: Library updates to `lodash` 4.17.11, `moment` 2.24.0 and `extend` 3.0.2
  Earlier lodash versions had this security vulnerability: https://nvd.nist.gov/vuln/detail/CVE-2018-16487
- Use select distinct when getting preserved actions while cleaning workflow instance history
- Dependency and plugin updates:
  - slf4j 1.7.26
  - spring 5.1.5.RELEASE
  - hamcrest 2.1
  - reactor.netty 0.8.5.RELEASE
  - swagger 1.5.22
  - mockito 2.24.5
  - io.dropwizard.metrics 4.0.5
  - mysql-connector-java 8.0.15
  - mssql-jdbc 7.2.1.jre8
  - hikaricp 3.3.1
  - maven-surefire 2.22.1
  - jetty 9.4.15.v20190215
  - h2 1.4.199
- Fix workflow history cleanup to keep the actions that hold the latest values of state variables
- nFlow Explorer: Custom content to workflow definition and workflow instance pages.
- nFlow Explorer: Executors page to use standard time formatting in tooltips
- nFlow netty: Add support for registering Spring ApplicationListeners
- nFlow jetty: Replace deprecated NCSARequestLog with CustomRequestLog
- Fix `WorkflowLifecycle.stop()` blocking forever if `nflow.autostart=false` and `WorkflowLifecycle.start()` not called

## 5.3.3 (2019-02-04)

**Details**
- Build using correct Java class version (was broken in 5.3.2)

## 5.3.2 (2019-02-04)

**Details**
- Fix workflow history cleanup to work also with MySQL 5.7
- Upgraded Spring to version 5.1.4.RELEASE

## 5.3.1 (2019-01-13)

**Details**
- Expose workflow instance history cleanup delay in REST API and show it in Explorer (workflow definition -> settings)
- Preserve actions that are parent actions for child workflows in workflow history cleanup (otherwise cascade foreign key deletes also the children)

## 5.3.0 (2019-01-03)

**Highlights**
- Add experimental DB2 support

**Breaking changes**
- nFlow `Datasource` uses now underlying database specific `Driver`s instead of `DataSource`s.
  Make a corresponding change, if you have customized `nflow.db.*.driver` parameters.

**Details**
- Popup notifications from workflow instance updates, network and authentication issues in Explorer
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
