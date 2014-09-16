## 1.X.X

* fixes and new features based on production needs
* more examples of nflow usage
* screencast of making example application

## 2.0.0 (Vuohi)

**target date 28.11.2014**

* nFlow radiator
  * pie chart for workflows in different states
  * graphs for visualizing incoming/processed workflow instances
* nFlow management UI
  * search workflow instances
  * update workflow instance state
  * visualization of workflow instances (incl. action history)
* workflow management
  * high-level locks - only one workflow against lock running at a time
  * subworkflow support
  * internal nFlow metastate for workflows (created, started, finished)?
* archive tables
* performance testing
* quickstart maven archetype
* improved PostgreSQL support
* optional support for flyway

## Future releases

* improved human workflow support
  * e.g. send ticket (http-link containing token) through email for opening  a form in which human task can be performed
* tools for generating workflow definition skeleton based on graph
* alarms (with configurable thresholds)
* additional data storage support
  * Oracle
  * MongoDB
  * DB2
