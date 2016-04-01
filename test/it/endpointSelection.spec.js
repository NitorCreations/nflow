'use strict';

var po = require('./pageobjects/pageobjects');

describe('endpoint selection', function () {
  var menu = po.menu({});
  var frontPage = po.frontPage({});

  beforeEach(function() { frontPage.get(); });

  it('endpoint menu should be closed', function() {
    expect(menu.isEnpointSelectionOpen()).toBe(false);
  });

  it('selected endpoint should be "local nflow instance"', function() {
    expect(menu.selectedEndpoint()).toBe('local nflow instance');
  });

  it('front page show localhost workflows', function() {
    expect(frontPage.getDefinitions()).toContain('fibonacci');
  });

  describe('clicking endpoint selection', function() {
    beforeEach(function() {
      menu.clickEndpointSelection();
    });

    it('endpoint menu should open', function() {
      expect(menu.isEnpointSelectionOpen()).toBe(true);
    });

    it('available endpoints should include nBank', function () {
      expect(menu.availableEndpoints()).toEqual(['nBank at nflow.io']);
    });

    describe('clicking open endpoint menu', function() {
      beforeEach(function() {
        menu.clickEndpointSelection();
      });

      it('endpoint menu should be closed', function() {
        expect(menu.isEnpointSelectionOpen()).toBe(false);
      });

    });
  });

  describe('selecting nBank endpoint', function() {
    beforeEach(function() {
      menu.clickEndpointSelection();
      menu.selectEndpoint('nbank');
    });

    afterEach(function() {
      menu.clickEndpointSelection();
      menu.selectEndpoint('localhost');

      expect(menu.selectedEndpoint()).toBe('local nflow instance');
      expect(frontPage.getDefinitions()).toContain('fibonacci');
    });

    it('endpoint menu should be closed', function() {
      expect(menu.isEnpointSelectionOpen()).toBe(false);
    });

    it('selected endpoint should be "nBank at nflow.io"', function() {
      expect(menu.selectedEndpoint()).toBe('nBank at nflow.io');
    });

    it('front page show localhost workflows', function() {
      expect(frontPage.getDefinitions()).toContain('withdrawLoan');
    });
  });

});
