# The `src` Directory

## Overview

The `src/` directory contains all code used in the application.

```
src/
  |- app/
  |  |- about/
  |  |- front-page/
  |  |- app.js
  |- external/
  |  |- angular-ui-bootstrap/
  |  |  |- <customized version for nflow-explorer>
  |  |- dagre-d3/
  |  |  |- <https://github.com/cpettitt/dagre-d3/issues/102>
  |  |  |- <Old version that is no longer available in repositories>
  |- images/
  |- styles/
  |- config.js
  |- index.html
```

- `src/app/` - application-specific code, i.e. code not likely to be reused in another application. [Read more &raquo;](app/README.md)
- `src/images/` - static images.
- `src/external/` - third-party libraries: customized or not available on Bower.
- `src/styles/` - (s)css.
- `src/config.js` - environment config file. See below.
- `src/index.html` - this is the HTML document of the single-page application. See below.

See each directory for a detailed explanation (if any).

## `index.html`

The `index.html` file is the HTML document of the single-page application (SPA)
that should contain all markup that applies to everything in the app, such as
the header and footer. It declares with `ngApp` that this is `nflowExplorer`,
specifies the topmost `NaviCtrl` controller, and contains the `ngView` directive
into which route templates are placed.

When adding third-party libraries or application modules, they need to be added to index.html to be picked up by the build system.

## `config.js`

The `config.js` file is environment configuration file in the form of vanilla javascript,
e.g. it is does not require angular, is not uglified and can be edited directly in the web server.
Build scripts may apply environment specific values into this file.
