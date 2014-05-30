# nFlow Goals and Non-Goals [![Build Status](https://travis-ci.org/NitorCreations/nflow.svg?branch=master)](https://travis-ci.org/NitorCreations/nflow)

nFlow is a light weight business process engine with emphasis on the following goals or features.

* **Conciseness:** effort put on making the writing the workflow definitions easy
* **Modularity:** you can pick the desired components from nFlow stack or use it as standalone server
* **Deployment:** startup in seconds instead of minutes, effort put on supporting many scenarios
 
nFlow non-goals are important to understand as well:

* **BPMN/BPEL Support:** excluded by the goal of conciseness
* **Full UI Support:** although read-only visualization of workflows is in future roadmap

# Table of Contents

* [Getting Started](#getting-started)
  * [1 Minute Guide](#one-minute-guide)
  * [Components](#components)
  * [Usage Scenarios](#usage-scenarios)
  * [Anatomy of a Workflow](#anatomy-of-workflow)
    * [Implementation Class and States Declarations](#implementation-class-and-states-declarations)
    * [Settings and State Transitions](#settings-and-state-transitions)
    * [State Handler Methods](#state-handler-methods)
  * [Setting Up Your nFlow](#setting-up-your-nflow)
* [Configuration](#configuration)
  * [nFlow Properties](#nflow-properties)
  * [Database](#database)
  * [Security](#security)
  * [Logging](#logging)
* [Other Stuff](#other-stuff)
  * [Versioning](#versioning)
  * [REST API](#rest-api)

# <a name="getting-started"></a>Getting Started

## <a name="one-minute-guide"></a>1 Minute Guide

Create a Maven project. Add the following to your  `pom.xml`. nFlow is available in Maven central repository. 

```xml
<dependency>
  <groupId>com.nitorcreations</groupId>
  <artifactId>nflow-jetty</artifactId>
  <version>0.1.0</version>
</dependency>
```
Create a class for starting nFlow in embedded Jetty using H2 memory database.

```java
import com.nitorcreations.nflow.jetty.StartNflow;

public class App {
  public static void main(String[] args) throws Exception {
    new StartNflow().startTcpServerForH2().startJetty(7500, "dev");
  }
}
```
That's it! Running *App* will start nFlow server though without any workflow definitions. See the next sections for creating your own workflow definitions.

## <a name="components"></a>Components

nFlow consist of the following main components, each having the previous component as a dependency.
 1. **nflow-engine** contains a multithreaded workflow dispatcher, Java API for managing workflows and the persistance layer implementation. 
 2. **nflow-rest** contains a JAX-RS compliant REST service implementation for exposing workflow management and query APIs.
 3. **nflow-jetty** contains an embeddable Jetty server for running nFlow with your custom workflows.

In addition, nflow-tests component contains integration tests over demo workflows.

## <a name="usage-scenarios"></a>Usage Scenarios

The following example scenarios illustrate how you can use nFlow with your applications.

### Scenario 1: Embedded Engine Only

![Scenario 1 picture](nflow-documentation/userguide/userguide-scenario-1.png)

### Scenario 2: Inside Your Application Server

![Scenario 2 picture](nflow-documentation/userguide/userguide-scenario-2.png)

### Scenario 3: Full nFlow Stack

![Scenario 3 picture](nflow-documentation/userguide/userguide-scenario-3.png)

## <a name="anatomy-of-workflow"></a>Anatomy of a Workflow

In nFlow terminology, you have workflow definitions and instances. A workflow definition is Java class that contains the implementation of a business process (e.g. credit application process). A workflow instance is a runtime instance of the business process (e.g. credit application from a certain customer). As a developer, you need to implement the workflow definition after which the workflow instances can be submitted through nflow-engine API or nflow-rest-api services.

A workflow can be composed of human tasks (e.g. accept application), technical tasks (e.g. call REST service) or both of these tasks. A simple workflow that involves creating a credit application, the credit decision, possible money transfer and finally closing the credit application is illustrated below. The Java code for `CreditApplicationWorkflow` can be found from [nflow-tests -module](nflow-tests/src/main/java/com/nitorcreations/nflow/tests/demo/CreditApplicationWorkflow.java).

![](nflow-documentation/userguide/userguide-example-workflow.png)

### <a name="implementation-class-and-states-declarations"></a>Implementation Class and States Declarations

`CreditApplicationWorkflow` begins by extends [`WorkflowDefinition`](nflow-engine/src/main/java/com/nitorcreations/nflow/engine/workflow/WorkflowDefinition.java) which is the base class for all workflow implementations in nFlow. The state space of the workflow is enumerated after the class declaration. In this example, the states are also given a type and documentation. The following state types are supported (`WorkflowStateType`-enumeration):
 * **start:** an entry point to the workflow
 * **manual:** requires external state update (usually a human task required)
 * **normal:** state is executed and retried automatically by nFlow
 * **end:** final state to which workflow instance has finished

Currently the state types are informational only and useful for visualization. 

```java
public class CreditApplicationWorkflow extends WorkflowDefinition<State> {
...
  public static enum State implements WorkflowState {
    createCreditApplication(start, "Credit application is persisted to database"),
    acceptCreditApplication(manual, "Manual credit decision is made"),
    grantLoan(normal, "Loan is created to loan system"),
    finishCreditApplication(normal, "Credit application status is set"),
    done(end, "Credit application process finished"),
    error(manual, "Manual processing of failed applications");
...
```
### <a name="settings-and-state-transitions"></a>Settings and State Transitions

Each workflow implementation must have the following properties set through base class constructor:
 * **name:** defines the name that is used when submitting new instances (_creditApplicationProcess_)
 * **default start state:** state from which new instances start by default (_createCreditApplication_)
 * **generic error state:** error state for generic failures (_error_)

Optionally you can also override default timing related settings through custom subclass of `WorkflowSettings` (_CreditApplicationWorkflowSettings_). Next you can define allowed state transitions through `permit()` which checks that the corresponding state handler methods exist.

```java
public CreditApplicationWorkflow() {
  super("creditApplicationProcess", createCreditApplication, error, 
      new CreditApplicationWorkflowSettings());
  permit(createCreditApplication, acceptCreditApplication);
  permit(acceptCreditApplication, grantLoan);
  permit(acceptCreditApplication, finishCreditApplication);
  permit(finishCreditApplication, done);
}
```

### <a name="state-handler-methods"></a>State Handler Methods

For each state there must exist a state handler method with the same name. The state handler method must be a `public` method that takes [`StateExecution`](nflow-engine/src/main/java/com/nitorcreations/nflow/engine/workflow/StateExecution.java) as an argument. `StateExecution` contains the main interface through which workflow implementation can interact with nFlow (see next section).

Optionally you can define `@StateVar`-annotated POJOs (must have zero argument constructor) or Java primitive types as additional arguments after `StateExecution`. The additional arguments are automatically persisted by nFlow after state execution and passed automatically to subsequent state handler methods (see state variables in next section). In `CreditApplicationWorkflow` class `WorkflowInfo` is instantiated automatically (`instantiateNull=true`) when `createCreditApplication`-method is entered. Values set in `createCreditApplication` are afterwards available in other state handler methods.

Each state handler method must define and schedule the next state execution. For instance, `CreditApplicationWorkflow.createCreditApplication()` defines that acceptCreditApplication-state is executed immediately next. Manual and final states (e.g. acceptCreditApplication and error) must unschedule themself.

```java
public void createCreditApplication(StateExecution execution, 
        @StateVar(instantiateNull=true, value=VAR_KEY) WorkflowInfo info) {
  ...
  info.applicationId = "abc";
  execution.setNextState(acceptCreditApplication, "Credit application created", now());
}

public void acceptCreditApplication(StateExecution execution, 
        @StateVar(value=VAR_KEY) WorkflowInfo info) {
  ...
  execution.setNextState(acceptCreditApplication, 
        "Expecting manual credit decision", null);
}

public void grantLoan(StateExecution execution, @StateVar(value=VAR_KEY) WorkflowInfo info)
public void finishCreditApplication(StateExecution execution, 
        @StateVar(value=VAR_KEY) WorkflowInfo info)
public void done(StateExecution execution, @StateVar(value=VAR_KEY) WorkflowInfo info)
public void error(StateExecution execution, @StateVar(value=VAR_KEY) WorkflowInfo info) {
  ...
  execution.setNextState(error);
}
```

## Interacting with nFlow

TODO
* StateExecution in more detail
* State variables
* Retrying

# <a name="configuration"></a>Configuration

## <a name="nflow-properties"></a>nFlow Properties

Default values for nFlow properties can be overridden by adding *<env>*.properties file to classpath and specifying *env* as system property. For instance, add *dev.properties* to classpath and add *-Denv=dev* to JVM startup parameters.

TODO: table of nFlow properties and default values

## <a name="database"></a>Database

PostgreSQL, MySQL/MariaDB and H2 supported...
Database structures initialized manually or automatically...
Description of database tables...

## <a name="security"></a>Security

Currently nFlow does not come with any security framework. You can add your own layer of security e.g. through Spring Security if you wish.

## <a name="logging"></a>Logging

nFlow implements logging via [SLF4J](http://www.slf4j.org/) API. [nflow-jetty](https://github.com/NitorCreations/nflow/tree/master/nflow-jetty) uses [Logback](http://logback.qos.ch/) as the logging implementation.

# <a name="other-stuff"></a>Other Stuff

## <a name="versioning"></a>Versioning

nFlow uses [Semantic Versioning Specification (SemVer)](http://semver.org/)

## <a name="rest-api"></a>REST API

