var Config = new function() {

  //------------------------------------------------------------------------
  // NOTE: FULL NFLOW EXPLORER CONFIGURATION DOCUMENTATION IN:
  // https://github.com/NitorCreations/nflow/wiki/Explorer-Configuration
  //------------------------------------------------------------------------

  /**
   * Controls how often data is polled from the selected nFlow REST API, when an automatically refreshing
   * UI element is displayed (e.g. active executors list)
   */
  this.refreshSeconds = 60;

  /**
   * Defines nFlow API REST API endpoints that Explorer will use. If there's more than >1 endpoints defined,
   * the navigation bar will display a dropdown for endpoint selection and the first defined endpoint
   * is selected by default.
   * 
   * Endpoint properties:
   * - id: unique technical identifier for the endpoint, can be arbitrary string (required)
   * - title: string shown in the endpoint selection dropdown (required)
   * - apiUrl: nFlow REST API base URL (required)
   * - docUrl: Swagger UI URL for nFlow REST API (optional)
   */
  this.nflowEndpoints = [
    {
      id: 'nbank',
      title: 'nBank at nflow.io',
      apiUrl: 'https://bank.nflow.io/nflow/api',
      docUrl: 'https://bank.nflow.io/nflow/ui/doc/'
    },
    {
      id: 'localhost',
      title: 'local nflow instance',
      apiUrl: 'http://localhost:7500/nflow/api',
      docUrl: 'http://localhost:7500/nflow/ui/doc/'
    },
  ];

  /**
   * Generate custom content for workflow definition details page.
   * Optional function that returns either a string or a Promise
   * that resolves to a string. The string is added to the page DOM.
   * See customInstanceContent for example.
   */
  this.customDefinitionContent = function(definition) {
    return null;
  };

  /**
   * Generate custom content for workflow instance details page.
   * Optional function that returns either a string or a Promise
   * that resolves to a string. The string is added to the page DOM.
   *
   * The intended purpose is:
   * - to create links to external, which contain additional data about the workflow.
   *   e.g.
   *   ```
   *   return '<a href="https://cms.service.com/content/' + workflow.businessKey + '">Open CMS</a>'
   *   ```
   * - fetch additional data from external systems and display it in the in nFlow Explorer UI:
   *   ```
   *   return fetch('https://api.service.com/content/' + workflow.businessKey)
   *            .then(result => {
   *              return result.json()
   *                .then(data => data.contentTitle)
   *            })
   *   ```
   */
  this.customInstanceContent = function(definition, workflow, parentWorkflow, childWorkflows) {
    return null;
  };

  // Replaces HTML page title by given string
  this.htmlTitle = undefined;

  // Replaces nFlow text in header by image in given location
  this.nflowLogoFile = undefined;

  // Replaces nFlow text in header by given text (nflowLogoFile takes precedence, if it is defined)
  this.nflowLogoTitle = undefined;

  /**
   * Customizes columns shown in workflow instance search, for example following shows workflow id and type (which are
   * always first columns by default) followed by business key, state variable "cron" and next activation.
   * this.searchResultColumns = [
   *   {
   *     field: 'businessKey',
   *     label: 'Business key'
   *   },
   *   {
   *     field: 'stateVariables.cron',
   *     label: 'CRON'
   *   },
   *   {
   *     field: 'nextActivation',
   *     label: 'Next activation',
   *     type: 'timestamp'
   *   }
   * ]
   */
  this.searchResultColumns = undefined;
};