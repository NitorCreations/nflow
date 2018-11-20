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
};
