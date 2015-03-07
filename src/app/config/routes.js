(function () {
  'use strict';

  var m = angular.module('nflowVisApp.config.routes', [
    'nflowVisApp.services',
    'ui.router'
  ]);

  m.config(function ($stateProvider, $urlRouterProvider) {
    $urlRouterProvider.otherwise('/');

    $stateProvider.state('frontPageTab', {
      abstract: true,
      template: '<div ui-view></div>'
    });

    $stateProvider.state('searchTab', {
      abstract: true,
      template: '<div ui-view></div>'
    });

    $stateProvider.state('aboutTab', {
      abstract: true,
      template: '<div ui-view></div>'
    });

    $stateProvider
      .state('frontPage', {
        parent: 'frontPageTab',
        url: '/',
        templateUrl: 'app/front-page/frontPage.html',
        controller: 'FrontPageCtrl as ctrl'
      })
      .state('search', {
        parent: 'searchTab',
        url: '/search?type&state',
        templateUrl: 'app/search/search.html',
        controller: 'SearchCtrl as ctrl',
        resolve: {
          definitions: function (WorkflowDefinitions) {
            return WorkflowDefinitions.query().$promise;
          }
        }
      })
      .state('about', {
        parent: 'aboutTab',
        url: '/about',
        templateUrl: 'app/about/about.html',
        controller: 'AboutCtrl'
      })
      .state('workflow-stats', {
        parent: 'frontPageTab',
        url: '/workflow-stats?type',
        templateUrl: 'app/workflow-stats/workflowStats.html',
        controller: 'RadiatorCtrl'
      })
      .state('workflow-definition', {
        parent: 'frontPageTab',
        url: '/workflow-definition/:type',
        templateUrl: 'app/workflow-definition/workflowDefinition.html',
        controller: 'WorkflowDefinitionCtrl as ctrl',
        resolve: {
          'GraphService': function ($q, GraphService) {
            // do not open UI before products are loaded i.e. the following promise resolved
            var defer = $q.defer();
            GraphService.getCss(defer);
            return defer.promise;
          },
          definition: function ($stateParams, WorkflowDefinitions) {
            return WorkflowDefinitions.get({type: $stateParams.type}).$promise.then(_.first);
          }
        }
      })
      .state('workflow', {
        parent: 'searchTab',
        url: '/workflow/:id',
        templateUrl: 'app/workflow/workflow.html',
        controller: 'WorkflowCtrl as ctrl',
        resolve: {
          'GraphService': function ($q, GraphService) {
            // do not open UI before products are loaded i.e. the following promise resolved
            var defer = $q.defer();
            GraphService.getCss(defer);
            return defer.promise;
          },
          workflow: function ($stateParams, Workflows) {
            return Workflows.get({id: $stateParams.id}).$promise;
          },
          definition: function (WorkflowDefinitions, workflow) {
            return WorkflowDefinitions.get({type: workflow.type}).$promise.then(_.first);
          }
        }
      });
  });

  m.run(function ($rootScope, $state) {
    $rootScope.$state = $state;
  });

})();
