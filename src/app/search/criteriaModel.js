(function () {
  'use strict';

  var m = angular.module('nflowVisApp.search.criteriaModel', []);

  m.factory('CriteriaModel', function() {
    var self  = {};
    self.model = {};
    self.initialize = initialize;
    self.toQuery = toQuery;
    self.isEmpty = isEmpty;
    self.onDefinitionChange = onDefinitionChange;

    return self;

    function initialize(typeAndStateName, definitions) {
      angular.copy({}, self.model);

      self.model.definition = ensureTypeInDefinitions(typeAndStateName.type, definitions);
      self.model.state = ensureStateNameInDefinitionStates(typeAndStateName.stateName, self.model.definition);
    }

    function toQuery() {
      var q = {};

      q.type = _.result(self.model.definition, 'type');
      q.state = _.result(self.model.state, 'name');
      _.defaults(q, _.omit(self.model, ['definition', 'state']));

      return omitNonValues(q);
    }

    function isEmpty() {
      return _.isEmpty(omitNonValues(self.model));
    }

    function onDefinitionChange() {
      self.model.state = ensureStateNameInDefinitionStates(_.result(self.model.state, 'name'), self.model.definition);
    }

    function ensureTypeInDefinitions(type, definitions) {
      return nonValueToNull(_.find(definitions, function (d) { return d.type === type; }));
    }

    function ensureStateNameInDefinitionStates(stateName, definition) {
      return definition ? nonValueToNull(_.find(definition.states, function (s) { return s.name === stateName; })) : null;
    }

    function omitNonValues(object) {
      return _.omit(object, function (v) { return _.isUndefined(v) || _.isNull(v); });
    }

    function nonValueToNull(v) {
      return v || null;
    }
  });
})();
