(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.WorkflowStatsPoller', [
    'nflowExplorer.services.WorkflowDefinitionService',
  ]);

  m.service('WorkflowStatsPoller', function WorkflowStatsPoller($rootScope, config, $interval,
                                                                WorkflowDefinitionService) {
    var tasks = {};

    function addStateData(type, time, stats) {
      var data = tasks[type].data;
      data.push([time, stats]);
      while(data.length > config.maxHistorySize) {
        data.shift();
      }
    }

    function updateStats(type) {
      WorkflowDefinitionService.getStats(type)
        .then(function(stats) {
          console.info('Fetched statistics for ' + type);
          addStateData(type, new Date(), stats);
          tasks[type].latest = stats;
          $rootScope.$broadcast('workflowStatsUpdated', type);
        })
        .catch(function() {
          console.error('Fetching workflow ' + type + ' stats failed');
          addStateData(type, new Date(), {});
          $rootScope.$broadcast('workflowStatsUpdated', type);
        });
    }

    this.start = function(type) {
      if(!tasks[type]) {
        tasks[type] = {};
        tasks[type].data = [];
        console.info('Start stats poller for ' + type + ' with period ' + config.radiator.pollPeriod + ' seconds');
        updateStats(type);
        tasks[type].poller = $interval(function() { updateStats(type); },
          config.radiator.pollPeriod * 1000);
        return true;
      }
      return false;
    };

    this.getLatest = function(type) {
      if(!tasks[type]) {
        return undefined;
      }
      return tasks[type].latest;
    };
  });
})();
