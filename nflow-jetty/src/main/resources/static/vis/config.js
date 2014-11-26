'use strict';
var Config = function() {
  this.nflowUrl = '/api';

  this.radiator = {
    // poll period in seconds
    pollPeriod: 15,
    // max number of items to keep in memory
    maxHistorySize: 10000
  };
};

