## Next release

* Performance test framework
* Performance improvements
* Support for sub-workflows
* Status for workflows (created, in progress, finished etc.)
* Type for workflow actions (normal, manual, recovery etc.)
* Improvement for handling not permitted state changes
* Add checksum for workflow definitions in database to allow easy comparison
* Improvement for handling invalid states
* Training material
* Marketing material
* Fixes and new features based on production needs

## Future releases

* Quickstart maven archetype
* Optional support for database migration tool
* RequestData validation based on workflow definition when inserting new workflow instances
* Support for other databases
* High-level locks - only one workflow instance against lock running at a time
* Archive tables
* Improved human workflow support
* Tools for generating workflow definition skeletons
* Human-friendly mode for REST API
* Immediate execution of new workflow instance (if not busy)
* Increase test coverage
* Screencast of making an example application
* Support alarms
* Support alarm configuration in Explorer
* Support WAR and EAR packaging
* Option to skip writing workflow action when updating workflow instance to database
* Switch from JodaTime to Java 8 Date and Time API
* Java client for nFlow REST API
* nFlow Eclipse plugin
* Replace CXF with Jersey
* Add package-descriptions to javadocs
* Design and order nFlow stickers
* Support large amount of results in workflow instance search
* Provide more examples on using nFlow in different ways
* Support specifying next activation time as delta instead of absolute time in API
* Guice module that starts nFlow engine
* Define allowed state changes with annotations
* Support multiple executor groups in one Explorer
* Align Explorer page "buttons" to left
* Avoid throwing generic RuntimeExceptions
* Add missing javadocs for public API
* Configuration to disable Swagger and/or Explorer
* Fork/join support
* Collect metrics from REST API
* Remove need for transactions when using PostgreSQL to allow enabling auto-commit
