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

  that.get = function(type, state) {
    var url = '/#/search';

    var t = 'type=' + type;
    var s = 'state=' + state;

    if (type && state) {
      url += '?' + t + '&' + s;
    } else if (type) {
      url += '?' + t;
    } else if (state) {
      url += '?' + s;
    }

    browser.get(url);
    expect(that.isDisplayed()).toBeTruthy();
  };

  that.getType = function() { return spec.getValue(spec.type); };
  that.getState = function() { return spec.getValue(spec.state); };
  that.getInstanceId = function() { return spec.getValue(spec.instanceId); };
  that.getBusinessKey = function() { return spec.getValue(spec.businessKey); };
  that.getExternalId = function() { return spec.getValue(spec.externalId); };

  that.setType = function(type) {
    spec.type.sendKeys(type);
  };

  that.getStateOptions = function() {
    return spec.state.$$('option').getText();
  };

  that.hasResults = function() {
    return spec.resultRows.count().then(function(count){ return count > 0; });
  };

  return that;
};
