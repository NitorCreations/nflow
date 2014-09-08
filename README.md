# nFlow [![Build Status](https://travis-ci.org/NitorCreations/nflow.svg?branch=master)](https://travis-ci.org/NitorCreations/nflow) [![Coverage Status](https://img.shields.io/coveralls/NitorCreations/nflow.svg)](https://coveralls.io/r/NitorCreations/nflow?branch=master)

[![Logo](https://github.com/NitorCreations/nflow/blob/master/nflow-documentation/nflow-logo.png)]

nFlow is a light weight business process engine with emphasis on the following goals or features.

* **Conciseness:** effort put on making the writing the workflow definitions easy
* **Modularity:** you can pick the desired components from nFlow stack or use it as standalone server
* **Deployment:** startup in seconds instead of minutes, effort put on supporting many scenarios
 
nFlow non-goals are important to understand as well:

* **BPMN/BPEL Support:** excluded by the goal of conciseness
* **Full UI Support:** although read-only visualization of workflows is in future roadmap


# <a name="getting-started"></a>Getting Started

## <a name="one-minute-guide"></a>1 Minute Guide

Create a Maven project. Add the following to your  `pom.xml`. nFlow is available in Maven central repository. 

```xml
<dependency>
  <groupId>com.nitorcreations</groupId>
  <artifactId>nflow-jetty</artifactId>
  <version>0.3.1</version>
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
Point your browser to [http://localhost:7500/ui](http://localhost:7500/ui) and you can use interactive online documentation for the nFlow REST API. 

See the next sections for creating your own workflow definitions.

## <a name="components"></a>Ok, I'm interested!

For a more thorough getting started guide, configurations, license information etc. checkout the [nFlow wiki pages](https://github.com/NitorCreations/nflow/wiki)!
