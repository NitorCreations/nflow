(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.WorkflowDefinitionService', [
    'ngResource',
  ]);

  m.service('WorkflowDefinitionService', function WorkflowDefinitionService(config, $resource, $http, $cacheFactory) {
    var api = this;
    api.get = get;
    api.list = list;
    api.getStats = getStats;

    var getCache = $cacheFactory('workflow-definition');
    function get(type) {
      var resource = $resource(config.nflowUrl + '/v1/workflow-definition',
        {type: '@type'},
        {'get': {isArray: true,
          method:  'GET',
          cache: getCache} });

      return resource.get({type: type}).$promise;
    }

    var listCache = $cacheFactory('workflow-definition-list');
    function list() {
      var resource = $resource(config.nflowUrl + '/v1/workflow-definition',
        {type: '@type'},
        {'query': {isArray: true,
          method:  'GET',
          cache: listCache} });

      return resource.query().$promise;
    }

    function getStats(type) {
      var stats = $resource(config.nflowUrl + '/v1/statistics/workflow/:type', {type: '@type'});
      return stats.get({type: type}).$promise;
    }

  });

})();
