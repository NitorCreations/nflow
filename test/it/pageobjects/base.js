'use strict';

var _ = require('lodash');

module.exports = function (spec) {

  spec.parent = function(element) {
    return element.element(by.xpath('..'));
  };

  spec.hasClasses = function(element, expected) {
    return element.getAttribute('class').then(function (classAttr) {
      var actual = classAttr.split(' ');
      return _.every(expected, function(cls) { return _.contains(actual, cls); } );
    });
  };

  spec.isDisplayed = function(element) {
    return element.then(function(e) { return e.isDisplayed(); }, function() { return false; } );
  };

  var that = {};

  that.isDisplayed = function() {
    return spec.isDisplayed(spec.view);
  };

  return that;
};
