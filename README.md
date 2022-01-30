<img src="./nflow-documentation/nflow-logo-dark.svg" height="100" width="195" />

nFlow is a battle-proven solution for orchestrating business processes. Depending on where you're coming from, you can view nFlow as any of the following:

* Microservices orchestrator (as in [Saga-pattern](https://microservices.io/patterns/data/saga.html))
* Guaranteed delivery computing
* Replacement for [business process engine](https://www.techopedia.com/definition/26689/business-process-engine-bpe)
* Persistent [finite-state machine](https://en.wikipedia.org/wiki/Finite-state_machine)

nFlow has been under development since 2014-01-14 and version 1.0.0 was released on 2014-09-13.

**Key features**

* Non-declarative &mdash; workflows are defined as code
* Visualization &mdash; workflows can be visualized in [nFlow Explorer](https://github.com/NitorCreations/nflow/tree/master/nflow-explorer)
* Embeddable &mdash; usually embedded as a library, but a standalone server is also provided
* High availability &mdash; the same workflows can be processed by multiple deployments
* Fault tolerant &mdash; automatic recovery if runtime environment crashes
* Atomic state updates &mdash; uses and requires a relational database for atomic state updates and locking
* Multiple databases supported &mdash; PostgreSQL, MySQL, Oracle, Microsoft SQL Server, DB2, H2
* Open Source under EUPL

# <a name="getting-started"></a>Getting Started

## <a name="one-minute-guide"></a>1 Minute Guide

Create a Maven project. Add the following to your  `pom.xml`. nFlow is available in the [Maven Central Repository](https://search.maven.org/search?q=g:io.nflow). 

```xml
<dependency>
  <groupId>io.nflow</groupId>
  <artifactId>nflow-jetty</artifactId>
  <version>7.4.0</version>
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

## <a name="supported-java-versions"></a>Supported Java versions

* \>= 8.0.0: Java 11 or newer (tested with OpenJDK 11 and 17)
* 4.0.0–7.x.x: Java 8 or newer (tested with OpenJDK 8 and 11)
* < 4.0.0: Java 7 or newer

## <a name="components"></a>Ok, I'm interested!

For a more thorough getting started guide, configurations, license information etc. checkout the [nFlow wiki pages](https://github.com/NitorCreations/nflow/wiki)! You can also look into a short [slide deck](https://github.com/NitorCreations/nflow/raw/master/nflow-documentation/presentations/nflow_presentation.pdf).

Discussion and questions are welcome to our forum [nflow-users](https://groups.google.com/forum/#!forum/nflow-users) in Google Groups.

