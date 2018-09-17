(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.WorkflowDefinitionService', [
    'nflowExplorer.services.RestHelper',
  ]);

  m.service('WorkflowDefinitionService', function WorkflowDefinitionService(config, RestHelper, $cacheFactory) {
    var api = this;
    api.get = get;
    api.list = list;
    api.getStats = getStats;

    var getCache = $cacheFactory('workflow-definition');

    function get(type) {
      return RestHelper.query({
        path: '/v1/workflow-definition',
        cache: getCache
      }, {type: type});
    }

    var listCache = $cacheFactory('workflow-definition-list');

    function list() {
      return RestHelper.query({
        path: '/v1/workflow-definition',
        cache: listCache
      });
    }

    function getStats(type) {
      return RestHelper.get({path: '/v1/statistics/workflow/:type'}, {type: type}, {type: '@type'});
    }

  });

})();
