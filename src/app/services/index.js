(function () {
  'use strict';

  angular.module('nflowExplorer.services', [
    'nflowExplorer.config',

    'nflowExplorer.services.ExecutorService',
    'nflowExplorer.services.GraphService',
    'nflowExplorer.services.Time',
    'nflowExplorer.services.WorkflowDefinitionService',
    'nflowExplorer.services.WorkflowDefinitionStats',
    'nflowExplorer.services.WorkflowService',
    'nflowExplorer.services.WorkflowStatsPoller',
  ]);

})();

