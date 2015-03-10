'use strict';

module.exports = function (spec) {
  var that = require('./base')(spec);

  spec.view = $('section.wd-front-page');

  that.get = function () {
    browser.get('/');
    expect(that.isDisplayed()).toBeTruthy();
  };

  return that;
};
