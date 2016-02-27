(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.WorkflowSearch', [
    'nflowExplorer.config',
    'ngResource'
  ]);

  m.factory('WorkflowSearch', function WorkflowSearchFactory($resource, config) {
    return $resource(config.nflowUrl + '/v1/workflow-instance');
  });

})();
