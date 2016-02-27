(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.WorkflowDefinitions', []);

  m.factory('WorkflowDefinitions', function WorkflowDefinitionsFactory($resource, config, $cacheFactory) {
    return $resource(config.nflowUrl + '/v1/workflow-definition',
      {type: '@type'},
      {'get': {isArray: true,
        method:  'GET',
        cache: $cacheFactory('workflow-definition')},
        'query': {isArray: true,
          method: 'GET',
          cache: $cacheFactory('workflow-definition-list')}
      });
  });

})();
