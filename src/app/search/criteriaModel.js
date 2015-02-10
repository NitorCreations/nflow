(function () {
  'use strict';

  var m = angular.module('nflowVisApp.search.criteriaModel', []);

  m.factory('CriteriaModel', function() {
    var self  = {};
    self.model = {};
    self.initialize = initialize;
    self.toQuery = toQuery;
    self.isEmpty = isEmpty;
    self.onTypeChange = onTypeChange;

    return self;

    function initialize(typeAndState, definitions) {
      angular.copy({}, self.model);

      self.model.type = ensureTypeInDefinitions(typeAndState.type, definitions);
      self.model.state = ensureStateNameInTypeStates(typeAndState.state, self.model.type);
    }

    function toQuery() {
      var q = _.clone(self.model);

      if (q.type) { q.type = q.type.type; }
      if (q.state) { q.state = q.state.name; }

      return omitNonValues(q);
    }

    function isEmpty() {
      return _.isEmpty(omitNonValues(self.model));
    }

    function onTypeChange() {
      self.model.state = ensureStateNameInTypeStates(_.result(self.model.state, 'name'), self.model.type);
    }

    function ensureTypeInDefinitions(type, definitions) {
      return nonValueToNull(_.find(definitions, function (d) { return d.type === type; }));
    }

    function ensureStateNameInTypeStates(stateName, type) {
      return type ? nonValueToNull(_.find(type.states, function (s) { return s.name === stateName; })) : null;
    }

    function omitNonValues(object) {
      return _.omit(object, function (v) { return _.isUndefined(v) || _.isNull(v); });
    }

    function nonValueToNull(v) {
      return v || null;
    }
  });
})();
