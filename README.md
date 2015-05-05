<img src="https://github.com/NitorCreations/nflow/blob/master/nflow-documentation/nflow-logo-no-borders.png" height="100" width="195" />

nFlow is a light-weight and modular solution for orchestrating processes using Java. Non-exhaustive list of nFlow usage scenarios:

* **Orchestration of microservices:** trending REST-based microservice architecture does not provide standardized solutions for composing services into reliable business processes. nFlow fills this gap by providing a framework for implementing workflows based on ideas like [idempotent retry](http://www.servicedesignpatterns.com/WebServiceInfrastructures/IdempotentRetry) and [finite state machine](http://en.wikipedia.org/wiki/Finite-state_machine).

* **Traditional business process engine:** although nFlow does not implement (bloated) BPMN/BPEL standards, it can be utilized as superfast business process engine that runs on Jetty and starts in seconds instead of minutes. Custom visualization tools can be implemented on top nFlow REST API while waiting for the development of 'official' nFlow tools. 

* **Asynchronous backend dispatcher:** submit tasks from UI to nFlow that executes or forwards them realiably following [Request/Acknowledge pattern](http://servicedesignpatterns.com/ClientServiceInteractions/RequestAcknowledge). nFlow engine can be embedded to your UI applications or run as an external service.

# <a name="getting-started"></a>Getting Started

## <a name="one-minute-guide"></a>1 Minute Guide

Create a Maven project. Add the following to your  `pom.xml`. nFlow is available in Maven central repository. 

```xml
<dependency>
  <groupId>com.nitorcreations</groupId>
  <artifactId>nflow-jetty</artifactId>
  <version>1.3.0</version>
</dependency>
```
Create a class for starting nFlow in embedded Jetty using H2 memory database.

```java
import com.nitorcreations.nflow.jetty.StartNflow;

public class App {
  public static void main(String[] args) throws Exception {
    new StartNflow().startJetty(7500, "local", "");
  }
}
```
That's it! Running `App` will start nFlow server though without any workflow definitions. 
Point your browser to [http://localhost:7500/doc/](http://localhost:7500/doc/) and you can use interactive online documentation for the nFlow REST API.

See the next sections for creating your own workflow definitions.

## <a name="components"></a>Ok, I'm interested!

For a more thorough getting started guide, configurations, license information etc. checkout the [nFlow wiki pages](https://github.com/NitorCreations/nflow/wiki)! You can also look into a short [slide deck](https://github.com/NitorCreations/nflow/raw/master/nflow-documentation/presentations/nflow_presentation.pdf).
