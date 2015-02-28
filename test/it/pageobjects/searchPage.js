'use strict';

module.exports = function (spec) {
  var that = require('./base')(spec);

  spec.view = $('section.wd-search');

  spec.type = element(by.model('ctrl.model.definition'));
  spec.state = element(by.model('ctrl.model.state'));
  spec.instanceId = element(by.model('ctrl.model.id'));
  spec.businessKey = element(by.model('ctrl.model.businessKey'));
  spec.externalId = element(by.model('ctrl.model.externalId'));
  spec.resultRows = $$('table#search-result tbody tr');

  that.getType = function() { return spec.getValue(spec.type); };
  that.getState = function() { return spec.getValue(spec.state); };
  that.getInstanceId = function() { return spec.getValue(spec.instanceId); };
  that.getBusinessKey = function() { return spec.getValue(spec.businessKey); };
  that.getExternalId = function() { return spec.getValue(spec.externalId); };

  that.hasResults = function() {
    return spec.resultRows.count().then(function(count){ return count > 0; });
  };

  return that;
};
