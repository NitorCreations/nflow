var Config = function () {
  this.nflowEndpoints = [
    {
      id: 'Full stack example',
      title: 'Full stack example API',
      apiUrl: '/rest'
    }
  ];

  this.radiator = {
    // poll period in seconds
    pollPeriod: 15,
    // max number of items to keep in memory
    maxHistorySize: 10000
  };
};
