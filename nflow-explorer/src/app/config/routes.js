(function () {
  'use strict';

  var m = angular.module('nflowExplorer.config.routes', [
    'nflowExplorer.services',
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

    $stateProvider.state('executorsTab', {
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
        url: '/search?type&state&parentWorkflowId',
        templateUrl: 'app/search/search.html',
        controller: 'SearchCtrl as ctrl',
        resolve: {
          definitions: function (WorkflowDefinitionService) {
            return WorkflowDefinitionService.list();
          }
        }
      })
      .state('executors', {
        parent: 'executorsTab',
        url: '/executors',
        templateUrl: 'app/executors/executors.html',
        controller: 'ExecutorsCtrl as ctrl'
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
          loadCss: loadCss,
          definition: function (WorkflowDefinitionService, $stateParams) {
            return getDefinition(WorkflowDefinitionService, $stateParams.type);
          }
        }
      })
      .state('workflow', {
        parent: 'searchTab',
        url: '/workflow/:id',
        templateUrl: 'app/workflow/workflow.html',
        controller: 'WorkflowCtrl as ctrl',
        resolve: {
          loadCss: loadCss,
          workflow: function (WorkflowService, $stateParams) {
            return WorkflowService.get($stateParams.id);
          },
          definition: function (WorkflowDefinitionService, workflow) {
            return getDefinition(WorkflowDefinitionService, workflow.type);
          },
          parentWorkflow: function(WorkflowService, workflow) {
            if(workflow.parentWorkflowId) {
              return WorkflowService.get(workflow.parentWorkflowId);
            }
            return undefined;
          },
          childWorkflows: function(WorkflowService, $stateParams) {
            return WorkflowService.query({parentWorkflowId: $stateParams.id});
          }
        }
      });

    function getDefinition(WorkflowDefinitionService, type) {
      return WorkflowDefinitionService.get(type).then(_.first);
    }

    function loadCss(GraphService) {
      return GraphService.loadCss();
    }

  });
})();
