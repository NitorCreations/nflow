'use strict';

var po = require('./pageobjects/pageobjects.js');

describe('search page', function () {

  var page = po.searchPage({});

  function assertForm(type, state, instanceId, businessKey, externalId) {
    expect(page.getType()).toBe(type ? type : '');
    expect(page.getState()).toBe(state ? state : '');
    expect(page.getInstanceId()).toBe(instanceId ? instanceId : '');
    expect(page.getBusinessKey()).toBe(businessKey ? businessKey:  '');
    expect(page.getExternalId()).toBe(externalId ? externalId: '');
  }

  describe('entry criteria: empty', function () {
    beforeEach(function () {
      page.get();
    });

    it('has empty search form', function () {
      assertForm();
    });

    it('has no results', function () {
      expect(page.hasResults()).toBeFalsy();
    });
  });

  describe('entry criteria: type', function () {
    beforeEach(function () {
      page.get('creditDecision');
    });

    it('has pre-populated search form', function () {
      assertForm('creditDecision');
    });

    it('has results', function () {
      expect(page.hasResults()).toBeTruthy();
    });
  });

  describe('entry criteria: state', function () {
    beforeEach(function () {
      page.get(undefined, 'approved');
    });

    it('has empty search form', function () {
      assertForm();
    });

    it('has no results', function () {
      expect(page.hasResults()).toBeFalsy();
    });
  });

  describe('entry criteria: type and state', function () {
    beforeEach(function () {
      page.get('creditDecision', 'approved');
    });

    it('has pre-populated search form', function () {
      assertForm('creditDecision', 'approved');
    });

    it('has results', function () {
      expect(page.hasResults()).toBeTruthy();
    });
  });

  describe('criteria: workflow type', function () {
    beforeEach(function () {
      page.get();
    });

    it('determines state options', function () {
      expect(page.getStateOptions()).toEqual(['-- All states --']);

      page.setType('creditDecision');

      expect(page.getStateOptions()).toEqual([
        '-- All states --',
        'internalBlacklist',
        'decisionEngine',
        'satQuery',
        'manualDecision',
        'approved',
        'rejected'
      ]);
    });
  });
});
