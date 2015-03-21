(function () {
  'use strict';

  angular.module('nflowExplorer.filters', [])
    .filter('reverse', function () {
      return function reverse(items) {
        if (!items) {
          return [];
        }
        return items.slice().reverse();
      };
    })
    .filter('fromNow', function () {
      return function fromNow(value) {
        if (!value) {
          return '';
        }
        try {
          return moment(value).fromNow();
        } catch (e) {
          return value;
        }
      };
    })
    .filter('fromNowOrNever', function () {
      return function fromNowOrNever(value) {
        if (!value) {
          return 'never';
        }
        try {
          return moment(value).fromNow();
        } catch (e) {
          return value;
        }
      };
    })
    .filter('prettyPrintJson', function () {
      return function prettyPrintJson(value) {
        try {
          return JSON.stringify(value, undefined, 2);
        } catch (e) {
          return value;
        }
      };
    })
    .filter('nullToZero', function () {
      return function nullToZero(value) {
        return value ? value : 0;
      };
    });
})();
