(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.WorkflowDefinitionStats', []);

  m.factory('WorkflowDefinitionStats', function WorkflowDefinitionStatsFactory($resource, config) {
    return $resource(config.nflowUrl + '/v1/statistics/workflow/:type',{type: '@type'});
  });

})();
