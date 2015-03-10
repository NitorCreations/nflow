'use strict';

module.exports = function (spec) {
  var that = require('./base')(spec);

  spec.view = $('section.wd-search');

  return that;
};
