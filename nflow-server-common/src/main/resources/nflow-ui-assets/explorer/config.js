'use strict';
var Config = new function() {
  this.nflowEndpoints = [
    {
      id: 'default',
      title: 'Default nFlow API',
      apiUrl: '../../api',
      docUrl: '../doc/'
    }
  ];

  this.radiator = {
    // poll period in seconds
    pollPeriod: 15,
    // max number of items to keep in memory
    maxHistorySize: 10000
  };
};
