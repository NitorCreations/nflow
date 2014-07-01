package com.nitorcreations.nflow.engine.service;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

public class QueryWorkflowInstances {

  public final List<Integer> ids;
  public final List<String> types;
  public final List<String> states;
  public final String businessKey;
  public final String externalId;
  public final boolean includeActions;

  QueryWorkflowInstances(Builder builder) {
    super();
    this.ids = new ArrayList<>(builder.ids);
    this.types = new ArrayList<>(builder.types);
    this.states = new ArrayList<>(builder.states);
    this.businessKey = builder.businessKey;
    this.externalId = builder.externalId;
    this.includeActions = builder.includeActions;
  }

  public static class Builder {
    List<Integer> ids = new ArrayList<>();
    List<String> types = new ArrayList<>();
    List<String> states = new ArrayList<>();
    String businessKey;
    String externalId;
    boolean includeActions;

    public Builder() {
    }

    public Builder addIds(Integer ... newIds) {
      this.ids.addAll(asList(newIds));
      return this;
    }

    public Builder addTypes(String ... newTypes) {
      this.types.addAll(asList(newTypes));
      return this;
    }

    public Builder addStates(String ... newStates) {
      this.states.addAll(asList(newStates));
      return this;
    }

    public Builder setBusinessKey(String businessKey) {
      this.businessKey = businessKey;
      return this;
    }

    public Builder setExternalId(String externalId) {
      this.externalId = externalId;
      return this;
    }

    public Builder setIncludeActions(boolean includeActions) {
      this.includeActions = includeActions;
      return this;
    }

    public QueryWorkflowInstances build() {
      return new QueryWorkflowInstances(this);
    }
  }

}
