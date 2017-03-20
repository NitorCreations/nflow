'use strict';

module.exports = function (spec) {
  var that = require('./base')(spec);

  spec.view = $('section.wd-front-page');
  spec.definitionTypes = $$('tr > td:first-child');

  that.get = function () {
    browser.get('/');
    expect(that.isDisplayed()).toBeTruthy();
  };

  that.getDefinitions = function() {
    return spec.definitionTypes.getText().then(function(types){ return types; });
  };

  that.goToDefinition = function(row) {
    spec.definitionTypes.get(row).click();
  };

  return that;
};
