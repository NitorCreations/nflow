'use strict';
var Config = function() {
  // these get overwritten in EndpointService
  this.nflowUrl = 'http://localhost:7500/nflow/api';
  this.nflowApiDocs = 'http://localhost:7500/nflow/ui/doc/';
  this.withCredentials = false;

  this.nflowEndpoints = [
    {
      id: 'localhost',
      title: 'local nflow instance',
      apiUrl: 'http://localhost:7500/nflow/api',
      docUrl: 'http://localhost:7500/nflow/ui/doc/'
    },
    {
      id: 'nbank',
      title: 'nBank at nflow.io',
      apiUrl: 'https://bank.nflow.io/nflow/api',
      docUrl: 'https://bank.nflow.io/nflow/ui/doc/'
    },
  ];

  // see: https://github.com/AzureAD/azure-activedirectory-library-for-js/wiki/Config-authentication-context#configurable-options
  this.adal = {
    // undocumented adal-option that controls authentication on globally
    requireADLogin: false,
    instance: 'https://login.microsoftonline.com/',
    tenant: 'Enter_your_tenant_name_here_e.g._contoso.onmicrosoft.com',
    clientId: 'Enter_your_client_ID_here_e.g._e9a5a8b6-8af7-4719-9821-0deef255f68e',
    popUp: false
  };

  this.radiator = {
    // poll period in seconds
    pollPeriod: 15,
    // max number of items to keep in memory
    maxHistorySize: 10000
  };

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

  // Replaces nFlow logo in header by image in given location
  this.nflowLogoFile = undefined;

  // When true, hides the sticky footer with copyright
  this.hideFooter = false;

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
