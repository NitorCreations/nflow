'use strict';
var Config = function() {
  //this.nflowUrl = 'http://localhost:7500/api/';
  this.nflowUrl = 'http://nbank.dynalias.com:80/nflow';

  //this.nflowApiDocs = 'http://localhost:7500/doc/';
  this.nflowApiDocs = 'http://nbank.dynalias.com:80/nflow/ui/';

  this.radiator = {
    // poll period in seconds
    pollPeriod: 15,
    // max number of items to keep in memory
    maxHistorySize: 10000
  };
};
