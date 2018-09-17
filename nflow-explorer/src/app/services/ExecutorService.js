(function () {
  'use strict';
  var m = angular.module('nflowExplorer.services.ExecutorService', [
    'nflowExplorer.config',
    'nflowExplorer.services.RestHelper',
  ]);

  m.service('ExecutorService', function ExecutorService(config, RestHelper, $interval) {
    var started = false;

    var api = this;
    api.list = list;
    api.start = start;
    api.executors = [];

    function list() {
      return RestHelper.query({path: '/v1/workflow-executor'});
    }

    function start() {
      if (!started) {
        started = true;
        console.info('Start executor poller with period ', config.radiator.pollPeriod, ' seconds');
        updateExecutors();
        $interval(updateExecutors, config.radiator.pollPeriod * 1000);
      }
      console.info('Executor poller already started');
    }

    function updateExecutors() {
      console.info('Fetching executors');
      api.list().then(function (executors) {
        angular.copy(executors, api.executors);
      });
    }
  });

})();
