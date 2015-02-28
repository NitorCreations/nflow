'use strict';

var po = require('./pageobjects/pageobjects');

describe('front page', function() {

  var frontPage = po.frontPage({});
  var workflowDefinitionPage = po.workflowDefinitionPage({});

  beforeEach(function() { frontPage.get(); });

  it('has list of workflow definitions', function() {
    expect(frontPage.getDefinitions()).toEqual(['creditDecision', 'processCreditApplication', 'withdrawLoan']);
  });

  it('provides navigation to workflow definitions', function() {
    frontPage.goToDefinition(0);
    expect(workflowDefinitionPage.isDisplayed()).toBeTruthy();
  });
});
