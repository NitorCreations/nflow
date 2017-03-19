(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.WorkflowService', [
    'nflowExplorer.config',
    'ngResource'
  ]);

  m.service('WorkflowService', function WorkflowService(config, $http, $resource) {
    var api = this;
    api.get = get;
    api.update = update;
    api.query = query;
    api.signal = signal;

    function get(workflowId) {
      return $http({
        url: config.nflowUrl + '/v1/workflow-instance/' + workflowId + '?include=actions,currentStateVariables,actionStateVariables',
      }).then(function(response) {
        return response.data;
      });
    }

    function update(workflowId, data) {
      return $http({
        url: config.nflowUrl + '/v1/workflow-instance/' + workflowId,
        method: 'PUT',
        data: data,
      }).then(function(response) {
        return response.data;
      });
    }

    function signal(workflowId, data) {
      return $http({
        url: config.nflowUrl + '/v1/workflow-instance/' + workflowId + '/signal',
        method: 'PUT',
        data: data,
      }).then(function(response) {
        return response.data;
      });
    }

    function query(queryCriteria) {
      var resource = $resource(config.nflowUrl + '/v1/workflow-instance');
      return resource.query(queryCriteria).$promise;
    }

  });

})();
