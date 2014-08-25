## 1.0.0 (Kuikka)

**target date 1.9.2014**

* developer support
  * separate and javadoc public apis clearly to own java packages
  * refine names of classes and packages
  * quickstart maven archetype
  * better user guide
* reliability improvements
  * automatic task recovery (based on dead node detection)
* workflow management
  * remove limit on single initial data variable -> make them normal state vars
  * do not execute end or manual states
* improved PostgreSQL support
* smaller improvements

## 1.X.X

* fixes and new features based on production needs
* more examples of nflow usage
* screencast of making example application

## 2.0.0 (Vuohi)

**target date 1.12.2014**

* nFlow radiator
  * search workflow instances
  * update workflow instance state
  * visualization of workflow instances (incl. action history)
  * nFlow statistics
* workflow management
  * high-level locks - only one workflow against lock running at a time
  * subworkflow support
  * internal nFlow metastate for workflows (created, started, finished)?
* improved human workflow support
  * e.g. send ticket (http-link containing token) through email for opening  a form in which human task can be performed
* additional data storage support
  * Oracle
  * MongoDB
* Archive tables
* performance testing
* admin features
  * optional support for flyway

## Future releases

* Tools for generating workflow definition skeleton based on graph
* Alarms (with configurable thresholds)
* Support for DB2
