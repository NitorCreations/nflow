'use strict';

/**
 * @ngdoc overview
 * @name nflowVisApp
 * @description
 * # nflowVisApp
 *
 * Main module of the application.
 */
angular
.module('nflowVisApp', [
  'nflowVisApp.about',
  'nflowVisApp.frontpage',
  'nflowVisApp.search',
  'nflowVisApp.services',
  'nflowVisApp.workflow',
  'nflowVisApp.workflow_definition',
  'nflowVisApp.workflow_stats',
  'ngAnimate',
  'ngCookies',
  'ngRoute',
  'ngSanitize',
  'ngTouch',
  'ui.bootstrap'
])
.config(function ($routeProvider) {
  $routeProvider
  .when('/', {
    templateUrl: 'scripts/frontpage/frontpage.html',
    controller: 'FrontpageCtrl',
    activeTab: 'frontpage'
  })
  .when('/search', {
    templateUrl: 'scripts/search/search.html',
    controller: 'WorkflowSearchCtrl',
    activeTab: 'search'
  })
  .when('/about', {
    templateUrl: 'scripts/about/about.html',
    controller: 'AboutCtrl',
    activeTab: 'about'
  })
  .when('/workflow-stats', {
    templateUrl: 'scripts/workflow_stats/workflow_stats.html',
    controller: 'RadiatorCtrl',
    activeTab: 'frontpage'
  })
  .when('/workflow-definition/:type', {
    templateUrl: 'scripts/workflow_definition/workflow_definition.html',
    controller: 'WorkflowDefinitionCtrl',
    activeTab: 'frontpage',
    resolve: {
      'GraphService': [ '$q', 'GraphService', function($q, GraphService) {
        // do not open UI before products are loaded i.e. the following promise resolved
        var defer = $q.defer();
        GraphService.getCss(defer);
        return defer.promise;
      }]
    }

  })
  .when('/workflow/:id', {
    templateUrl: 'scripts/workflow/workflow.html',
    controller: 'WorkflowCtrl',
    activeTab: 'search',
    resolve: {
      'GraphService': [ '$q', 'GraphService', function($q, GraphService) {
        // do not open UI before products are loaded i.e. the following promise resolved
        var defer = $q.defer();
        GraphService.getCss(defer);
        return defer.promise;
      }]
    }
  })
  .otherwise({
    redirectTo: '/'
  });
})
.run(function($rootScope, $route){
  var path = function() {
    if($route && $route.current && $route.current.$$route) {
      return $route.current.$$route.activeTab;
    }
    return '';
  };
  $rootScope.$watch(path, function(newVal){
    $rootScope.activeTab = newVal;
  });
  $rootScope.isActiveTab = function(tab) {
    return $rootScope.activeTab === tab;
  };
})
.run(function(ExecutorPoller) {
  ExecutorPoller.start();
})
.controller('NaviCtrl', function($scope, $location) {
  // nope, $routeParams.radiator wont work here
  $scope.radiator = !!$location.search().radiator;
})
.filter('reverse', function() {
  return function reverse(items) {
    if(!items) {
      return [];
    }
    return items.slice().reverse();
  };
})
.filter('fromNow', function() {
  return function fromNow(value) {
    if(!value) {
      return '';
    }
    try {
      return moment(value).fromNow();
    } catch(e){
      return value;
    }
  };
})
.filter('fromNowOrNever', function() {
  return function fromNowOrNever(value) {
    if(!value) {
      return 'never';
    }
    try {
      return moment(value).fromNow();
    } catch(e){
      return value;
    }
  };
})
.filter('prettyPrintJson', function() {
  return function prettyPrintJson(value) {
    try {
      return JSON.stringify(value, undefined, 2);
    } catch(e) {
      return value;
    }
  };
})
.filter('nullToZero', function() {
  return function nullToZero(value) {
    return value ? value : 0;
  };
});
