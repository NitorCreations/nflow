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
.factory('ManageWorkflow', function ManageWorkflowFactory($http, config) {
  function processAction(id, actionDescription, action) {
    var url = config.nflowUrl + '/v1/workflow-instance/' + id + '/' + action;
    if (actionDescription) {
      url = url.concat('?actionDescription=' + encodeURIComponent(actionDescription));
    }
    return $http.put(url);
  }
  return {
    stop: function (id, actionDescription) {
      return processAction(id, actionDescription, 'stop');
    },
    pause: function (id, actionDescription) {
      return processAction(id, actionDescription, 'pause');
    },
    resume: function (id, actionDescription) {
      return processAction(id, actionDescription, 'resume');
    }
  };
})
.factory('WorkflowSearch', function WorkflowSearchFactory($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-instance');
})
.factory('Executors', function ExecutorsFactory($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-executor');
})
.factory('WorkflowDefinitions', function WorkflowDefinitionsFactory($resource, config, $cacheFactory) {
  return $resource(config.nflowUrl + '/v1/workflow-definition',
                   {type: '@type'},
                   {'get': {isArray: true,
                            method:  'GET',
                            cache: $cacheFactory('workflow-definition')},
                    'query': {isArray: true,
                              method: 'GET',
                              cache: $cacheFactory('workflow-definition-list')}
                   });
})
.factory('WorkflowDefinitionStats', function WorkflowDefinitionStatsFactory($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-definition/:type/statistics',{type: '@type'});
})
.service('GraphService', function GraphServiceFactory($http, $rootScope) {
  this.getCss = function getCss(defer) {
    // links are relative to displayed page
    $http.get('styles/data/graph.css')
    .success(function(data) {
      $rootScope.graph = {};
      $rootScope.graph.css=data;
      defer.resolve();
    })
    .error(function() {
      console.warn('Failed to load graph.css');
      $rootScope.graph = {};
      defer.resolve();
    });
  };
})
.service('ExecutorPoller', function ExecutorPollerService($rootScope, config, Executors, $interval) {
  var task = {};

  function updateExecutors() {
    Executors.query(function(executors) {
      console.info('Fetching executors');
      // TODO should store to this variable,
      // then in controller $scope.executors = ExecutorPoller.getExecutors();
      $rootScope.executors = executors;
      $rootScope.$broadcast('executorsUpdated');
    });
  }
  this.start = function() {
    if(!task.poller) {
      console.info('Start executor poller with period ' + config.radiator.pollPeriod + ' seconds');
      updateExecutors();
      task.poller = $interval(updateExecutors, config.radiator.pollPeriod * 1000);
      return true;
    }
    return false;
  };
})
.service('WorkflowStatsPoller', function WorkflowStatsPoller($rootScope, config, $interval,
                                                              WorkflowDefinitions, WorkflowDefinitionStats) {
  var tasks = {};

  function addStateData(type, time, stats) {
    var data = tasks[type].data;
    data.push([time, stats]);
    while(data.length > config.maxHistorySize) {
      data.shift();
    }
  }

  function updateStats(type) {
    WorkflowDefinitionStats.get({type: type},
                                function(stats) {
                                  console.info('Fetched statistics for ' + type);
                                  addStateData(type, new Date(), stats);
                                  tasks[type].latest = stats;
                                  $rootScope.$broadcast('workflowStatsUpdated', type);
                                },
                                function() {
                                  console.error('Fetching workflow ' + type + ' stats failed');
                                  addStateData(type, new Date(), {});
                                  $rootScope.$broadcast('workflowStatsUpdated', type);
                                });
  }

  this.start = function(type) {
    if(!tasks[type]) {
      tasks[type] = {};
      tasks[type].data = [];
      console.info('Start stats poller for ' + type + ' with period ' + config.radiator.pollPeriod + ' seconds');
      updateStats(type);
      tasks[type].poller = $interval(function() { updateStats(type); },
                                     config.radiator.pollPeriod * 1000);
      return true;
    }
    return false;
  };

  this.getLatest = function(type) {
    if(!tasks[type]) {
      return undefined;
    }
    return tasks[type].latest;
  };
})
;
