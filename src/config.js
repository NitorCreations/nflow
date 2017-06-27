'use strict';
var Config = function() {
  // these get overwritten in EndpointService
  this.nflowUrl = 'http://localhost:7500/api/nflow';
  this.nflowApiDocs = 'http://localhost:7500/doc/';

  this.nflowEndpoints = [
    {
      id: 'localhost',
      title: 'local nflow instance',
      apiUrl: 'http://localhost:7500/api/nflow',
      docUrl: 'http://localhost:7500/doc/'
    },
    {
      id: 'nbank',
      title: 'nBank at nflow.io',
      apiUrl: 'https://bank.nflow.io/nflow/api/nflow',
      docUrl: 'https://bank.nflow.io/nflow/doc/'
    },
  ];

  this.radiator = {
    // poll period in seconds
    pollPeriod: 15,
    // max number of items to keep in memory
    maxHistorySize: 10000
  };
};
