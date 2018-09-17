'use strict';

describe('Directive: workflowTabStateVariables', function () {

  var scope, element;

  beforeEach(module('nflowExplorer.components.filters'));
  beforeEach(module('nflowExplorer.karma.templates'));
  beforeEach(module('nflowExplorer.workflow.tabs.stateVariables'));

  beforeEach(inject(function($rootScope, $compile) {
    scope = $rootScope.$new();
    element = '<workflow-tab-state-variables workflow="workflow"></workflow-tab-state-variables>';
    element = $compile(element)(scope);
  }));

  var setWorkflowStateVariables = function(stateVariables) {
    scope.workflow = {
      stateVariables: stateVariables
    };
    scope.$digest();
  };

  it('Workflow has no state variables', function () {
    setWorkflowStateVariables(undefined);
    expect(element.find('.test-state-variables-no-variables').hasClass('ng-hide')).toBe(false);
    expect(element.find('.test-state-variables-has-variables').hasClass('ng-hide')).toBe(true);
  });

  it('Workflow has state variables', function () {
    setWorkflowStateVariables({
        foo: 'bar'
    });
    expect(element.find('.test-state-variables-no-variables').hasClass('ng-hide')).toBe(true);
    expect(element.find('.test-state-variables-has-variables').hasClass('ng-hide')).toBe(false);
    expect(element.find('.test-state-variables-has-variables > tbody > tr > td:nth-child(1)').html()).toContain('foo');
    expect(element.find('.test-state-variables-has-variables > tbody > tr > td:nth-child(2)').html()).toContain('bar');
  });
});
