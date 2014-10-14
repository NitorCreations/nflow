'use strict';
/**
 * Display single workflow instance
 */

var app = angular.module('nflowVisApp');

app.factory('Workflows', function ($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-instance/:id',
                   {id: '@id', include: 'actions,currentStateVariables,actionStateVariables'},
                   {'update': {method: 'PUT'},
                   });
});

app.controller('WorkflowCtrl', function ($scope, Workflows, WorkflowDefinitions, $routeParams) {
    Workflows.get({id: $routeParams.id},
                          function(workflow) {
                            $scope.workflow = workflow;
                            console.debug(workflow);

                            WorkflowDefinitions.get({type: workflow.type},
                                                   function(data) {
                                                     $scope.definition = _.first(data);
                                                     console.debug($scope.definition);
                                                   });

                          });

  $scope.getClass = function getClass(action) {
    http://getbootstrap.com/css/#tables
    return "";
  };

  $scope.selectAction = function selectAction(action) {
    console.log("Action selected", action);
  };

  $scope.duration = function duration(action) {
    var start = moment(action.executionStartTime);
    var end = moment(action.executionEndTime);
    var d = moment.duration(end.diff(start));
    if(d < 1000) {
      return d + " msec";
    }
    return d.humanize();
  };
});
