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
  'nflowVisApp.frontPage',
  'nflowVisApp.search',
  'nflowVisApp.services',
  'nflowVisApp.workflow',
  'nflowVisApp.workflowDefinition',
  'nflowVisApp.workflowStats',
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
    templateUrl: 'app/front-page/frontPage.html',
    controller: 'FrontPageCtrl',
    activeTab: 'frontPage'
  })
  .when('/search', {
    templateUrl: 'app/search/search.html',
    controller: 'WorkflowSearchCtrl',
    activeTab: 'search'
  })
  .when('/about', {
    templateUrl: 'app/about/about.html',
    controller: 'AboutCtrl',
    activeTab: 'about'
  })
  .when('/workflow-stats', {
    templateUrl: 'app/workflow-stats/workflowStats.html',
    controller: 'RadiatorCtrl',
    activeTab: 'frontPage'
  })
  .when('/workflow-definition/:type', {
    templateUrl: 'app/workflow-definition/workflowDefinition.html',
    controller: 'WorkflowDefinitionCtrl',
    activeTab: 'frontPage',
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
    templateUrl: 'app/workflow/workflow.html',
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
