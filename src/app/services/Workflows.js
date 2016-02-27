(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.Workflows', []);

  m.factory('Workflows', function WorkflowsFactory($resource, config) {
    return $resource(config.nflowUrl + '/v1/workflow-instance/:id',
      {id: '@id', include: 'actions,currentStateVariables,actionStateVariables'},
      {'update': {method: 'PUT'},
      });
  });

})();
