'use strict';
var Config = function() {
  //this.nflowUrl = 'http://localhost:7500';
  this.nflowUrl = 'http://nbank.dynalias.com:80/nflow';

  this.radiator = {
    // poll period in seconds
    pollPeriod: 15,
    // max number of items to keep in memory
    maxHistorySize: 10000
  };
};


