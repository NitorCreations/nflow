var Config = new function() {
  this.refreshSeconds = 60;

  this.nflowEndpoints = [
    {
      id: 'localhost',
      title: 'local nflow instance',
      apiUrl: '/nflow/api',
      docUrl: '/nflow/ui/doc/'
    },
  ];
}
