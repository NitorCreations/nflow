var Config = function () {
  this.nflowUrl = 'http://localhost:8080/nflow/api';
  this.nflowApiDocs = 'http://localhost:8080/nflow/ui/doc/';
  this.radiator = {
    // poll period in seconds
    pollPeriod: 15,
    // max number of items to keep in memory
    maxHistorySize: 10000
  };
};