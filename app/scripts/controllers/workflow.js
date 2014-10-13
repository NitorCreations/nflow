'use strict';
/**
 * Display single workflow instance
 */

var app = angular.module('nflowVisApp');

app.factory('Workflows', function ($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-instance/:id',
                   {id: '@id', include: 'actions'},
                   {'update': {method: 'PUT'},
                   });
});

app.controller('WorkflowCtrl', function ($scope, Workflows, $routeParams) {
    Workflows.get({id: $routeParams.id},
                          function(data) {
                            console.debug(data);
                          });
});
