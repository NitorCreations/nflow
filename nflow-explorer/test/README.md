# The `test` Directory

## Quick Start

All the following command are supposed to be run in the project root.

### Unit tests
```sh
$ grunt test
```

### Protractor tests

Update webdriver:
```sh
$ node_modules/protractor/bin/webdriver-manager update
```

Build nFlow in repository root (in order to run test backend):
```sh
$ mvn -DskipTests install
```

Start nFlow test backend in separate terminal (change version number in filename):
```sh
$ java -jar nflow-tests/target/nflow-tests-6.0.1-SNAPSHOT.jar
```

Start standalone Selenium in nflow-explorer:
```sh
$ npm run selenium
```

Note that the tests require also [nBank in AWS](https://bank.nflow.io/) to be running.

To run protractor tests:
```
$ grunt itest
```
To run protractor tests against distribution build:
```sh
$ grunt itest:dist
```

## Overview

Contains unit and end-to-end tests.
```
test/
  |- itest/
  |  |- pageobjects/
  |  |- frontPage.spec.js
  |  ...
  |- spec/
  |- karma.conf.js
  |- protractor.conf.js
```
- `itest` - protractor tests and pageobjects
- `spec` - unit tests
- `karma.conf.js` - unit test configuration
- `protractor.conf.js` - protractor test configuration
