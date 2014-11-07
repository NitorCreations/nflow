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
});
