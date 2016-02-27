(function () {
  'use strict';
  var m = angular.module('nflowExplorer.services.Executors', [
    'nflowExplorer.config',
    'ngResource',
  ]);

  m.factory('Executors', function ExecutorsFactory($resource, config) {
    return $resource(config.nflowUrl + '/v1/workflow-executor');
  });

})();
