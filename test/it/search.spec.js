'use strict';

var po = require('./pageobjects/pageobjects');
var fixture = require('./fixture');

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
      page.get(fixture.wfs[0].name);
    });

    it('has pre-populated search form', function () {
      assertForm(fixture.wfs[0].name);
    });

    it('has results', function () {
      expect(page.hasResults()).toBeTruthy();
    });
  });

  describe('entry criteria: state', function () {
    beforeEach(function () {
      page.get(undefined, fixture.wfs[0].states[0]);
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
      page.get(fixture.wfs[0].name, fixture.wfs[0].withActionHistory.state);
    });

    it('has pre-populated search form', function () {
      assertForm(fixture.wfs[0].name, fixture.wfs[0].withActionHistory.state);
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

      page.setType(fixture.wfs[0].name);

      expect(page.getStateOptions()).toEqual([ '-- All states --'].concat(fixture.wfs[0].states));
    });
  });
});
