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

  this.radiator = {
    // poll period in seconds
    pollPeriod: 15,
    // max number of items to keep in memory
    maxHistorySize: 10000
  };
};
