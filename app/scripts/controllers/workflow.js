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
  /** called when node is clicked */
  function nodeSelected(nodeId) {
    console.debug('Selecting node ' + nodeId);
    if($scope.selectedNode) {
      unhiglightNode($scope.graph, $scope.definition, $scope.selectedNode);
    }
    if(nodeId) {
      higlightNode($scope.graph, $scope.definition, nodeId);
    }
    $scope.selectedNode = nodeId;
  }

  $scope.nodeSelected = nodeSelected;
  Workflows.get({id: $routeParams.id},
                function(workflow) {
                  $scope.workflow = workflow;
                  console.debug(workflow);

                  WorkflowDefinitions.get({type: workflow.type},
                                          function(data) {
                                            $scope.definition = _.first(data);
                                            console.debug($scope.definition);
                                            $scope.graph = workflowDefinitionGraph($scope.definition);
                                            // must use $apply() - event not managed by angular
                                            function nodeSelectedCallBack(nodeId) {
                                              $scope.$apply(function() {
                                                nodeSelected(nodeId);
                                              });
                                            }
                                            drawWorkflowDefinition($scope.graph, 'workflowSvg', nodeSelectedCallBack);

                                          });

                });

  $scope.getClass = function getClass(action) {
    // See http://getbootstrap.com/css/#tables
    return '';
  };

  $scope.selectAction = function selectAction(action) {
    var state = action;
    if(typeof(action) !== 'string') {
      state = action.state;
    }
    console.log('Action selected', state);
    nodeSelected(state)
  };

  $scope.duration = function duration(action) {
    var start = moment(action.executionStartTime);
    var end = moment(action.executionEndTime);
    if(!start || !end) {
      return "-";
    }
    var d = moment.duration(end.diff(start));
    if(d < 1000) {
      return d + ' msec';
    }
    return d.humanize();
  };

  $scope.nextActivation = function nextActivation() {
    if(!$scope.workflow) {
      return "";
    }
    if(!$scope.workflow.nextActivation) {
      return "never";
    }
    return moment($scope.workflow.nextActivation).max(new Date()).fromNow();
  };

  $scope.currentStateSince = function currentStateSince() {
    if(!$scope.workflow) {
      return "";
    }
    var lastAction = _.last($scope.workflow.actions);
    if(!lastAction) {
      return "";
    }
    return moment(lastAction.executionEndTime).fromNow();
  };
});
