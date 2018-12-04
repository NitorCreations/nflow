<img src="https://github.com/NitorCreations/nflow/blob/master/nflow-documentation/nflow-logo-no-borders.png" height="100" width="195" />

nFlow is a battle-proven solution for orchestrating business processes. Depending on where you're coming from, you can view nFlow as any of the following:

* Microservices orchestrator (as in [Saga-pattern](https://microservices.io/patterns/data/saga.html))
* Guaranteed delivery computing
* Replacement for [business process engine](https://www.techopedia.com/definition/26689/business-process-engine-bpe)
* Persistent [finite-state machine](https://en.wikipedia.org/wiki/Finite-state_machine)

**Key features and benefits**

* Non-declarative - workflows are defined as code
* Visualization - workflows can be visualized in [nFlow Explorer](https://github.com/NitorCreations/nflow/tree/master/nflow-explorer)
* Embeddable and modular - usually embedded as a library, but a full-blown standalone server and add-on libraries are also provided
* High availability - workflows can be shared by multiple nFlow deployments
* Fault tolerant - automatic recovery after runtime environment crashes
* Atomic state updates - uses and requires a relational database for atomic state updates and locking
* Multiple databases supported - PostgreSQL, MySQL, Oracle, Microsoft SQL Server, H2
* Open Source under EUPL

# <a name="getting-started"></a>Getting Started

## <a name="one-minute-guide"></a>1 Minute Guide

Create a Maven project. Add the following to your  `pom.xml`. nFlow is available in the [Maven Central Repository](https://search.maven.org/search?q=g:io.nflow). 

```xml
<dependency>
  <groupId>io.nflow</groupId>
  <artifactId>nflow-jetty</artifactId>
  <version>5.2.0</version>
</dependency>
```
Create a class for starting nFlow in embedded Jetty using H2 memory database.

```java
import io.nflow.jetty.StartNflow;

public class App {
  public static void main(String[] args) throws Exception {
    new StartNflow().startJetty(7500, "local", "");
  }
}
```
That's it! Running `App` in your favourite IDE will start nFlow server though without any workflow definitions. 
Point your browser to [http://localhost:7500/nflow/ui/doc/](http://localhost:7500/nflow/ui/doc/) and you can use interactive online documentation for the nFlow REST API.
Point your browser to [http://localhost:7500/nflow/ui/explorer/](http://localhost:7500/nflow/ui/explorer/) and you can use nFlow Explorer.

See the next sections for creating your own workflow definitions.

Note! Starting from 4.0.0 release, nFlow requires Java 8 or newer. Older releases require Java 7 or newer.

## <a name="components"></a>Ok, I'm interested!

For a more thorough getting started guide, configurations, license information etc. checkout the [nFlow wiki pages](https://github.com/NitorCreations/nflow/wiki)! You can also look into a short [slide deck](https://github.com/NitorCreations/nflow/raw/master/nflow-documentation/presentations/nflow_presentation.pdf).

Discussion and questions are welcome to our forum [nflow-users](https://groups.google.com/forum/#!forum/nflow-users) in Google Groups.

