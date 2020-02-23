(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.WorkflowService', [
    'nflowExplorer.config',
    'nflowExplorer.services.RestHelper',
  ]);

  m.service('WorkflowService', function WorkflowService(config, RestHelper) {
    var api = this;
    api.get = get;
    api.update = update;
    api.query = query;
    api.signal = signal;

    var getStateVariablesInQuery = false;
    if (config.searchResultColumns) {
      getStateVariablesInQuery = config.searchResultColumns.find(function (column) {
        return column.field.startsWith('stateVariables');
      });
    }

    function get(workflowId) {
      return RestHelper.get({
        path: '/v1/workflow-instance/id/' + workflowId + '?include=actions,currentStateVariables,actionStateVariables&queryArchive=true'
      });
    }

    function update(workflowId, data) {
      return RestHelper.update('/v1/workflow-instance/id/' + workflowId, data);
    }

    function signal(workflowId, data) {
      return RestHelper.update('/v1/workflow-instance/' + workflowId + '/signal', data);
    }

    function query(queryCriteria) {
      return RestHelper.query({path: '/v1/workflow-instance' + (getStateVariablesInQuery ? '?include=currentStateVariables' : '')}, queryCriteria);
    }

  });

})();
