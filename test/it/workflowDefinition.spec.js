'use strict';

// definition, graph: verify clicks
// definition, tab:all instances: verify list

var po = require('./pageobjects/pageobjects');

describe('workflow definition page', function () {

  var page = po.workflowDefinitionPage({});
  var searchPage = po.searchPage({});

  beforeEach(function() { page.get('creditApplication'); });

  it('provides navigation to instance search by type', function () {
    page.toInstanceSearchByType();
    expect(searchPage.isDisplayed()).toBeTruthy();
  });

  xit('clicking node in graph hi-lights node', function() {});

  xdescribe('tabs', function() {
    describe('active instances', function() {});

    describe('all instances', function() {
      it('clicking instance hi-lights corresponding node in graph', function() {});

      it('provides navigation to instance search by type and state', function() {});
    });

    describe('workflow settings', function() {});

    describe('radiator', function() {});
  });

});
