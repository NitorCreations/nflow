'use strict';

module.exports = function (spec) {
  var that = require('./base')(spec);

  spec.view = $('section.wd-workflow-definition');

  that.get = function () {
    // TODO parameterize by type
    browser.get('/#/workflow-definition/creditDecision');
    expect(that.isDisplayed()).toBeTruthy();
  };

  return that;
};
