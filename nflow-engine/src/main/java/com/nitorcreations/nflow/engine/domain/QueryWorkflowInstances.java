package com.nitorcreations.nflow.engine.domain;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

public class QueryWorkflowInstances {

  public final List<String> types;
  public final List<String> states;
  public final String businessKey;
  public final String externalId;
  public final boolean includeActions;

  private QueryWorkflowInstances(Builder builder) {
    super();
    this.types = new ArrayList<>(builder.types);
    this.states = new ArrayList<>(builder.states);
    this.businessKey = builder.businessKey;
    this.externalId = builder.externalId;
    this.includeActions = builder.includeActions;
  }

  public static class Builder {
    List<String> types = new ArrayList<>();
    List<String> states = new ArrayList<>();
    String businessKey;
    String externalId;
    boolean includeActions;

    public Builder() {
    }

    public Builder addTypes(String ... types) {
      this.types.addAll(asList(types));
      return this;
    }

    public Builder addStates(String ... states) {
      this.states.addAll(asList(states));
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
