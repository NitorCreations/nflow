'use strict';
angular.module('nflowVisApp.services',
               ['ngResource'])
.constant('config', new Config())
.factory('Workflows', function ($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-instance/:id',
                   {id: '@id', include: 'actions,currentStateVariables,actionStateVariables'},
                   {'update': {method: 'PUT'},
                   });
})
.factory('WorkflowSearch', function ($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-instance');
})
.factory('Executors', function($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-executor');
})
.factory('WorkflowDefinitions', function ($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-definition',
                   {type: '@type'},
                   {'get': {isArray: true,
                            method:  'GET'}
                   });
})
.factory('WorkflowDefinitionStats', function($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-definition/:type/statistics',{type: '@type'});
});
