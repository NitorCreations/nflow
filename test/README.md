# The `test` Directory

## Quick Start

All the following command are supposed to be run in the project root.

### Unit tests
```sh
$ grunt test
```

### Protractor tests
```sh
$ node_modules/protractor/bin/webdriver-manager update
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
