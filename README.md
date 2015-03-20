# nflow-explorer

User Interface for [nFlow](https://github.com/NitorCreations/nflow).
***

## Demo

http://nbank.dynalias.com/nflow/explorer/

## Quick Start

Install Node.js and then:

```sh
$ sudo npm  -g install grunt-cli karma bower
$ npm install
$ bower install
$ grunt
$ grunt serve
```

## Naming Conventions

Category|Convention|Example
--------|----------|-------
folder name|lisp-case|  src/app/front-page
file name|camelCase|frontPage.js
angular module|camelCase|angular.module('nflowExplorer.frontPage', ...
angular controller|PascalCase|.controller('FrontPageCtrl', ...
angular directive|camelCase|.directive('workflowExecutorTable', ...
angular service|PascalCase|.factory('ManageWorkflow', ...

## Overall Directory Structure

```
nflow-explorer/
  |- dist/
  |- src/
  |  |- app/
  |  |  |- <app logic>
  |  |- external/
  |  |  |- <third-party libraries: customized or not available on Bower>
  |  |- images/
  |  |  |- <static image files>
  |  |- styles/
  |  |  |- main.scss
  |- test/
  |- bower_components/
  |- .bowerrc
  |- bower.json
  |- Gruntfile.js
  |- package.json
```

- `dist/` - release is built into this directory.
- `src/` - our application sources. [Read more &raquo;](src/README.md)
- `test/` - test sources and configuration. [Read more &raquo;](test/README.md)
- `bower_components/` - third-party libraries. [Bower](http://bower.io) will install packages here. Anything added to this directory will need to be manually
  added to `src/index.html` and `test/karma.conf.js` to be picked up by the build system.
- `.bowerrc` - the Bower configuration file. This tells Bower to install components into the `bower_components/` directory.
- `bower.json` - this is our project configuration for Bower and it contains the list of Bower dependencies we need.
- `Gruntfile.js` - our build script.
- `package.json` - metadata about the app, used by NPM and our build script. Our NPM dependencies are listed here.
