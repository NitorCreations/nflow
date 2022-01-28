'use strict';

describe('Service: CriteriaModel', function () {
  var CriteriaModel,
    definitions,
    actualModel;

  beforeEach(module('nflowExplorer.search.criteriaModel'));
  beforeEach(inject(function (_CriteriaModel_) {
    CriteriaModel = _CriteriaModel_;
    definitions = [
      { type: 'foo', states: [ {  id: 'bar'}] }
    ];

    actualModel = CriteriaModel.model;
    expect(actualModel).toEqual({});
  }));

  describe('initialize', function () {
    it('sets fields with empty input', function () {
      CriteriaModel.initialize({}, definitions);
      expect(actualModel).toEqual({ definition: null, state: null, parentWorkflowId: null,
                                    status: null, businessKey: null, externalId: null, id: null });
    });

    it('sets definition matching to type', function () {
      CriteriaModel.initialize({ type: 'foo'}, definitions);
      expect(actualModel).toEqual({ definition: definitions[0], state: null, parentWorkflowId: null,
                                    status: null, businessKey: null, externalId: null, id: null });
    });

    it('sets unknown definition as null', function () {
      CriteriaModel.initialize({ type: 'not in definitions'}, definitions);
      expect(actualModel).toEqual({ definition: null, state: null, parentWorkflowId: null,
                                    status: null, businessKey: null, externalId: null, id: null });
    });

    it('sets known definition state', function () {
      CriteriaModel.initialize({ type: 'foo', stateId: 'bar'}, definitions);
      expect(actualModel).toEqual({ definition: definitions[0], state: definitions[0].states[0], parentWorkflowId: null,
                                    status: null, businessKey: null, externalId: null, id: null });
    });

    it('sets unknown state to null', function () {
      CriteriaModel.initialize({ type: 'foo', stateId: 'not in foo states'}, definitions);
      expect(actualModel).toEqual({ definition: definitions[0], parentWorkflowId: null, state: null,
                                    status: null, businessKey: null, externalId: null, id: null });
    });

    it('sets parentWorkflowId to parsed integer', function () {
      CriteriaModel.initialize({parentWorkflowId: '123'}, definitions);
      expect(actualModel).toEqual({ definition: null, state: null, parentWorkflowId: '123',
                                    status: null, businessKey: null, externalId: null, id: null });
    });

    it('input properties other than type, state and parentWorkflowId are ignored', function () {
      CriteriaModel.initialize({ foo: 'bar'}, definitions);
      expect(actualModel).toEqual({ definition: null, state: null, parentWorkflowId: null,
                                    status: null, businessKey: null, externalId: null, id: null });
    });
  });

  describe('toQuery', function () {
    it('sets definition type when available', function () {
      actualModel.definition = definitions[0];
      expect(CriteriaModel.toQuery()).toEqual({type: 'foo', queryArchive: true});

      delete actualModel.definition.type;
      expect(CriteriaModel.toQuery()).toEqual({queryArchive: true});

      delete actualModel.definition;
      expect(CriteriaModel.toQuery()).toEqual({queryArchive: true});

    });

    it('sets state id', function () {
      actualModel.state = definitions[0].states[0];
      expect(CriteriaModel.toQuery()).toEqual({state: 'bar', queryArchive: true});

      delete actualModel.state.id;
      expect(CriteriaModel.toQuery()).toEqual({queryArchive: true});

      delete actualModel.state;
      expect(CriteriaModel.toQuery()).toEqual({queryArchive: true});
    });

    it('null values are omitted', function () {
      actualModel.foo = null;
      expect(CriteriaModel.toQuery()).toEqual({queryArchive: true});
    });

    it('undefined values are omitted', function () {
      actualModel.foo = undefined;
      expect(CriteriaModel.toQuery()).toEqual({queryArchive: true});
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

  describe('onDefinitionChange', function () {
    it('when definition is unset, state is unset', function () {
      actualModel.definition = null;
      actualModel.state = definitions[0].states[0];
      actualModel.foo = 'bar';
      CriteriaModel.onDefinitionChange();
      expect(actualModel).toEqual({ definition: null, state: null, foo: 'bar' });
    });

    it('state is unset if it is not included in definition states', function () {
      actualModel.definition = definitions[0];
      actualModel.state = { id: 'not in definition states'};
      CriteriaModel.onDefinitionChange();
      expect(actualModel).toEqual({ definition: definitions[0], state: null});
    });

    it('state is maintained if is included in definitions states', function () {
      actualModel.definition = definitions[0];
      actualModel.state = definitions[0].states[0];
      CriteriaModel.onDefinitionChange();
      expect(actualModel).toEqual({ definition: definitions[0], state: definitions[0].states[0]});
    });

    it('null state is handled', function () {
      actualModel.definition = definitions[0];
      actualModel.state = null;
      CriteriaModel.onDefinitionChange();
      expect(actualModel).toEqual({ definition: definitions[0], state: null});
    });

  });
});
