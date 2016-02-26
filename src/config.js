'use strict';
var Config = function() {
  this.nflowUrl = 'http://localhost:7500/api/nflow';
  //this.nflowUrl = 'http://bank.nflow.io/nflow/api/nflow';

  this.nflowApiDocs = 'http://localhost:7500/doc/';
  //this.nflowApiDocs = 'http://bank.nflow.io/nflow/doc/';

  this.radiator = {
    // poll period in seconds
    pollPeriod: 15,
    // max number of items to keep in memory
    maxHistorySize: 10000
  };
};
