# The `src/app` Directory

## Overview

```
src/
  |- app/
  |  |- main/         (startup configs, routes)
  |  |- about/        (about page)
  |  |- front-page/   (front page)
  |  |- layout/       (overall application layout, e.g. header, content, footer)
  |  |- search/       (search page)
  |  ...
  |  |- components/   (reusable components)
  |  |- services/     (angular services [and resources?])
  |  |- app.js
```

The `src/app` directory contains all code specific to this application. Apart
from `app.js`, this directory is filled with subdirectories corresponding to 
high-level sections of the application, often corresponding to top-level routes. 
Each directory can have as many subdirectories as it needs, and the build system will understand what to
do. For example, a top-level route might be "products", which would be a folder
within the `src/app` directory that conceptually corresponds to the top-level
route `/products`, though this is in no way enforced. Products may then have
subdirectories for "create", "view", "search", etc. The "view" submodule may
then define a route of `/products/:id`, ad infinitum.

- TODO clarify services and resources
- TODO where to put generic utils like filters, currently they are in app.js, possibilities include app/main and 
  [components/util](https://jhipster.github.io/using_angularjs.html)
