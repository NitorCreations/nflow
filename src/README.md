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
  |  |- dygraphs
  |  |  |- <?>
  |- images/
  |- styles/
  |- config.js
  |- index.html
```

- `src/app/` - application-specific code, i.e. code not likely to be reused in another application. [Read more &raquo;](app/README.md)
- `src/images/` - static images. 
- `src/external/` - third-party libraries that have been customized, see reasons above.
- `src/styles/` - (s)css.
- `src/index.html` - this is the HTML document of the single-page application. See below.

See each directory for a detailed explanation (if any).

## `index.html`

The `index.html` file is the HTML document of the single-page application (SPA)
that should contain all markup that applies to everything in the app, such as
the header and footer. It declares with `ngApp` that this is `nflowVisApp`,
specifies the topmost `NaviCtrl` controller, and contains the `ngView` directive
into which route templates are placed.

When adding third-party libraries or application modules, they need to be added to index.html to be picked up by the build system.
