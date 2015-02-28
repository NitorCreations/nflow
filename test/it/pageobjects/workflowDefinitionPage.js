'use strict';

module.exports = function (spec) {
  var that = require('./base')(spec);

  spec.view = $('section.wd-workflow-definition');
  spec.instanceSearchByTypeLink = by.linkText('Search related workflow instances');

  that.get = function (type) {
    browser.get('/#/workflow-definition/' + type);
    expect(that.isDisplayed()).toBeTruthy();
  };

  that.toInstanceSearchByType = function() {
    element(spec.instanceSearchByTypeLink).click();
  };

  return that;
};
