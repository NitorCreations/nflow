(function () {
  'use strict';

var m = angular.module('nflowVisApp.workflow', []);
  m.controller('WorkflowCtrl', function WorkflowCtrl($scope, Workflows, ManageWorkflow, WorkflowDefinitions, $routeParams, $rootScope) {
    $scope.manage = {};
    $scope.manage.timeUnits = ['minutes','hours','days'];
    $scope.manage.timeUnit = $scope.manage.timeUnits[0];
    $scope.manage.duration = 0;

    $scope.getClass = getClass;
    $scope.selectAction = selectAction;
    $scope.duration = duration;
    $scope.currentStateTime= currentStateTime;
    $scope.updateWorkflow = updateWorkflow;
    $scope.stopWorkflow = stopWorkflow;
    $scope.pauseWorkflow = pauseWorkflow;
    $scope.resumeWorkflow = resumeWorkflow;

    function defaultNextState(stateName) {
      $scope.manage.nextState = _.first(_.filter($scope.definition.states, function(state) {
        return state.name === stateName;
      }));
    }

    function nodeSelected(nodeId) {
      console.debug('Selecting node ' + nodeId);
      if($scope.selectedNode) {
        unhiglightNode($scope.graph, $scope.definition, $scope.selectedNode, $scope.workflow);
      }
      if(nodeId) {
        higlightNode($scope.graph, $scope.definition, nodeId, $scope.workflow);
      }
      $scope.selectedNode = nodeId;
      defaultNextState(nodeId);
    }

    function readWorkflow() {
      Workflows.get({id: $routeParams.id},
                    function(workflow) {
                      $scope.workflow = workflow;
                      console.debug('Workflow', workflow);

                      WorkflowDefinitions.get({type: workflow.type},
                                              function(data) {
                                                $scope.definition = _.first(data);
                                                console.debug('Definition', $scope.definition);

                                                $scope.graph = workflowDefinitionGraph($scope.definition, $scope.workflow);
                                                // must use $apply() - event not managed by angular
                                                function nodeSelectedCallBack(nodeId) {
                                                  $scope.$apply(function() {
                                                    nodeSelected(nodeId);
                                                  });
                                                }

                                                defaultNextState(workflow.state);

                                                drawWorkflowDefinition($scope.graph, '#workflowSvg', nodeSelectedCallBack, $rootScope.graph.css);
                                                markCurrentState(workflow);
                                              });

                    });
    }
    readWorkflow();

    function getClass(action) {
      // See http://getbootstrap.com/css/#tables
      if(!action.type) {
        return '';
      }
      return {'stateExecution' : 'success',
              'stateExecutionFailed' :'danger',
              'externalChange' : 'info',
              'recovery': 'warning'}[action.type];
    }

    function selectAction(action) {
      var state = action;
      if(typeof(action) !== 'string') {
        state = action.state;
      }
      console.log('Action selected', state);
      nodeSelected(state);
    }

    function duration(action) {
      var start = moment(action.executionStartTime);
      var end = moment(action.executionEndTime);
      if(!start || !end) {
        return '-';
      }
      var d = moment.duration(end.diff(start));
      if(d < 1000) {
        return d + ' msec';
      }
      return d.humanize();
    }

    function currentStateTime() {
      if(!$scope.workflow) {
        return '';
      }
      var lastAction = _.last($scope.workflow.actions);
      if(!lastAction) {
        return '';
      }
      return lastAction.executionEndTime;
    }

    function updateWorkflow(manage) {
      console.info('updateWorkflow()', manage);
      var now = moment(new Date());
      var request = {};
      if(manage.nextState) {
        request.state = manage.nextState.name;
      }
      if((manage.duration !== undefined && manage.duration !== null) && manage.timeUnit) {
        request.nextActivationTime = now.add(moment.duration(manage.duration, manage.timeUnit));
      }
      if(manage.actionDescription) {
        request.actionDescription = manage.actionDescription;
      }
      Workflows.update({id: $routeParams.id},
                       request,
                       function() {
                         readWorkflow();
                       });
    }

    function stopWorkflow(manage) {
      console.info('stopWorkflow()', manage);
      ManageWorkflow.stop($routeParams.id, manage.actionDescription).then(readWorkflow);
    }

    function pauseWorkflow(manage) {
      console.info('pauseWorkflow()', manage);
      ManageWorkflow.pause($routeParams.id, manage.actionDescription).then(readWorkflow);
    }

    function resumeWorkflow(manage) {
      console.info('resumeWorkflow()', manage);
      ManageWorkflow.resume($routeParams.id, manage.actionDescription).then(readWorkflow);
    }
  });
})();
