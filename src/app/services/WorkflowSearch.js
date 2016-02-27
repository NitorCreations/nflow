(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.WorkflowSearch', []);

  m.factory('WorkflowSearch', function WorkflowSearchFactory($resource, config) {
    return $resource(config.nflowUrl + '/v1/workflow-instance');
  });

})();
