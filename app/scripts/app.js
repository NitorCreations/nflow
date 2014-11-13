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
  'nflowVisApp.services',
  'nflowVisApp.radiator',
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
    templateUrl: 'views/main.html',
    controller: 'MainCtrl',
    activeTab: 'main'
  })
  .when('/search', {
    templateUrl: 'views/search.html',
    controller: 'WorkflowSearchCtrl',
    activeTab: 'search'
  })
  .when('/about', {
    templateUrl: 'views/about.html',
    controller: 'AboutCtrl',
    activeTab: 'about'
  })
  .when('/radiator', {
    templateUrl: 'views/radiator.html',
    controller: 'RadiatorCtrl',
    activeTab: 'main'
  })
  .when('/workflow-definition/:type', {
    templateUrl: 'views/workflow_definition.html',
    controller: 'WorkflowDefinitionCtrl',
    activeTab: 'main',
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
    templateUrl: 'views/workflow.html',
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
  $rootScope.$watch(path, function(newVal, oldVal){
    $rootScope.activeTab = newVal;
  });
  $rootScope.isActiveTab = function(tab) {
    return $rootScope.activeTab === tab;
  };
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
