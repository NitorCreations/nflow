'use strict';

describe('Service: CriteriaModel', function () {
  var CriteriaModel,
    definitions,
    actualModel;

  beforeEach(module('nflowVisApp.search.criteriaModel'));
  beforeEach(inject(function (_CriteriaModel_) {
    CriteriaModel = _CriteriaModel_;
    definitions = [
      { type: 'foo', states: [ {  name: 'bar'}] }
    ];

    actualModel = CriteriaModel.model;
    expect(actualModel).toEqual({});
  }));

  describe('initialize', function () {
    it('sets type and state as nulls with empty input', function () {
      CriteriaModel.initialize({}, definitions);
      expect(actualModel).toEqual({ type: null, state: null });
    });

    it('sets definition matching to type', function () {
      CriteriaModel.initialize({ type: 'foo'}, definitions);
      expect(actualModel).toEqual({ type: definitions[0], state: null });
    });

    it('sets unknown type as null', function () {
      CriteriaModel.initialize({ type: 'not in definitions'}, definitions);
      expect(actualModel).toEqual({ type: null, state: null });
    });

    it('sets definition state matching to input definition and state', function () {
      CriteriaModel.initialize({ type: 'foo', state: 'bar'}, definitions);
      expect(actualModel).toEqual({ type: definitions[0], state: definitions[0].states[0] });
    });

    it('sets unknown state to null', function () {
      CriteriaModel.initialize({ type: 'foo', state: 'not in foo states'}, definitions);
      expect(actualModel).toEqual({ type: definitions[0], state: null });
    });

    it('properties other than type and state are ignored', function () {
      CriteriaModel.initialize({ foo: 'bar'}, definitions);
      expect(actualModel).toEqual({ type: null, state: null });
    });
  });

  describe('toQuery', function () {
    it('type is included if set', function () {
      actualModel.type = definitions[0];
      expect(CriteriaModel.toQuery()).toEqual({type: 'foo'});
    });

    it('state is included if set', function () {
      actualModel.state = definitions[0].states[0];
      expect(CriteriaModel.toQuery()).toEqual({state: 'bar'});
    });

    it('null values are omitted', function () {
      actualModel.foo = null;
      expect(CriteriaModel.toQuery()).toEqual({});
    });

    it('undefined values are omitted', function () {
      actualModel.foo = undefined;
      expect(CriteriaModel.toQuery()).toEqual({});
    });
  });

  describe('isEmpty', function () {
    it('is false when model has values', function () {
      actualModel.foo = 'bar';
      expect(CriteriaModel.isEmpty()).toBe(false);
    });

    it('is true when model is empty', function () {
      expect(CriteriaModel.isEmpty()).toBe(true);
    });

    it('is false when model has nulls', function () {
      actualModel.foo = null;
      expect(CriteriaModel.isEmpty()).toBe(true);
    });

    it('is false when model has undefined values', function () {
      actualModel.foo = undefined;
      expect(CriteriaModel.isEmpty()).toBe(true);
    });
  });

  describe('onTypeChange', function () {
    it('when type is unset, state is unset', function () {
      actualModel.type = null;
      actualModel.state = definitions[0].states[0];
      actualModel.foo = 'bar';
      CriteriaModel.onTypeChange();
      expect(actualModel).toEqual({ type: null, state: null, foo: 'bar' });
    });

    it('state is unset if it is not included in definition states', function () {
      actualModel.type = definitions[0];
      actualModel.state = { name: 'not in definition states'};
      CriteriaModel.onTypeChange();
      expect(actualModel).toEqual({ type: definitions[0], state: null});

    });

    it('state is maintained if is included in definitions states', function () {
      actualModel.type = definitions[0];
      actualModel.state = definitions[0].states[0];
      CriteriaModel.onTypeChange();
      expect(actualModel).toEqual({ type: definitions[0], state: definitions[0].states[0]});
    });
  });
});
