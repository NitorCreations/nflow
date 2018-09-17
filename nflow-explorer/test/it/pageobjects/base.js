'use strict';

var _ = require('lodash');

module.exports = function (spec) {

  spec.parent = function(element) {
    return element.element(by.xpath('..'));
  };

  spec.hasClasses = function(element, expected) {
    return element.getAttribute('class').then(function (classAttr) {
      var actual = classAttr.split(' ');
      return _.every(expected, function(cls) { return _.includes(actual, cls); } );
    });
  };

  spec.isDisplayed = function(element) {
    return element.isDisplayed();
  };

  spec.getValue = function(element) {
    var value = element.getAttribute('value');
    return value || element.getText();
  };

  var that = {};

  that.isDisplayed = function() {
    return spec.isDisplayed(spec.view);
  };

  return that;
};
