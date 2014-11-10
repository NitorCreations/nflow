'use strict';
angular.module('nflowVisApp.services',
               ['ngResource'])
.constant('config', new Config())
.factory('Workflows', function WorkflowsFactory($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-instance/:id',
                   {id: '@id', include: 'actions,currentStateVariables,actionStateVariables'},
                   {'update': {method: 'PUT'},
                   });
})
.factory('WorkflowSearch', function WorkflowSearchFactory($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-instance');
})
.factory('Executors', function ExecutorsFactory($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-executor');
})
.factory('WorkflowDefinitions', function WorkflowDefinitionsFactory($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-definition',
                   {type: '@type'},
                   {'get': {isArray: true,
                            method:  'GET'}
                   });
})
.factory('WorkflowDefinitionStats', function WorkflowDefinitionStatsFactory($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-definition/:type/statistics',{type: '@type'});
})
.service('GraphService', function GraphServiceFactory($http, $rootScope, $q) {
  this.getCss = function getCss(defer) {
    // links are relative to displayed page
    $http.get('styles/data/graph.css')
    .success(function(data) {
      $rootScope.graph = {};
      $rootScope.graph.css=data;
      defer.resolve();
    })
    .error(function(data) {
      console.warn('Failed to load graph.css');
      $rootScope.graph = {};
      defer.resolve();
    });
  };

});
