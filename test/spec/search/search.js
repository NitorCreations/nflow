'use strict';

describe('Controller: SearchCtrl', function () {
  var $controller,
    CriteriaModel;

  beforeEach(module('nflowExplorer.search'));

  beforeEach(inject(function (_$controller_, _CriteriaModel_) {
    $controller = _$controller_;
    CriteriaModel = _CriteriaModel_;
  }));

  function getCtrl(CriteriaModel) {
    return $controller('SearchCtrl', {
      $stateParams: {type: 'expected type', state: 'expected state name'},
      definitions: ['expected definition'],
      CriteriaModel: CriteriaModel
    });
  }

  it('sets definitions into view model', function () {
    var ctrl = getCtrl(CriteriaModel);
    expect(ctrl.definitions).toEqual(['expected definition']);
  });

  it('sets empty results into view model', function () {
    var ctrl = getCtrl(CriteriaModel);
    expect(ctrl.results).toEqual([]);
  });

  it('initializes criteria model from route params', function () {
    var mock = sinon.mock(CriteriaModel);
    var expectation = mock.expects('initialize').withExactArgs(
      {type: 'expected type', stateName: 'expected state name'},
      ['expected definition']
    );

    getCtrl(CriteriaModel);

    expectation.verify();
    mock.restore();
  });

  describe('hasResults', function () {
    var ctrl;

    beforeEach(function () {
      ctrl = getCtrl(CriteriaModel);
    });

    it('is false when results is empty', function () {
      expect(ctrl.hasResults()).toBe(false);
    });

    it('is true when results is not empty', function () {
      ctrl.results = ['result'];
      expect(ctrl.hasResults()).toBe(true);
    });
  });


});
