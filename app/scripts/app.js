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
    'ngAnimate',
    'ngCookies',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngTouch'
  ])
  .config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })
      .when('/about', {
        templateUrl: 'views/about.html',
        controller: 'AboutCtrl'
      })
      .when('/workflow-definition/:type', {
        templateUrl: 'views/workflow_definition.html',
        controller: 'WorkflowDefinitionCtrl'
      })
      .when('/workflow/:id', {
        templateUrl: 'views/workflow.html',
        controller: 'WorkflowCtrl'
      })
      .otherwise({
        redirectTo: '/'
      });
  });
