'use strict';

var _ = require('lodash');

var po = require('./pageobjects/pageobjects');
var fixture = require('./fixture');

describe('front page', function() {

  var frontPage = po.frontPage({});
  var workflowDefinitionPage = po.workflowDefinitionPage({});

  beforeEach(function() { frontPage.get(); });

  it('has list of workflow definitions', function() {
    expect(frontPage.getDefinitions()).toEqual(_.pluck(fixture.wfs, 'name'));
  });

  it('provides navigation to workflow definitions', function() {
    frontPage.goToDefinition(0);
    expect(workflowDefinitionPage.isDisplayed()).toBeTruthy();
  });
});
