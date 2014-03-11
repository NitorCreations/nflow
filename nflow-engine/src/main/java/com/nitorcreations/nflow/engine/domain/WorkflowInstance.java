package com.nitorcreations.nflow.engine.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

public class WorkflowInstance {

  public final Integer id;
  public final String type;
  public final String businessKey;
  public final String state;
  public final String stateText;
  public final DateTime nextActivation;
  public final boolean processing;
  public final String requestData;
  public final Map<String, String> stateVariables;
  public final Map<String, String> originalStateVariables = new HashMap<>();
  public final List<WorkflowInstanceAction> actions;
  public final int retries;
  public final DateTime created;
  public final DateTime modified;
  public final String owner;

  private WorkflowInstance(Builder builder) {
    super();
    this.id = builder.id;
    this.type = builder.type;
    this.businessKey = builder.businessKey;
    this.state = builder.state;
    this.stateText = builder.stateText;
    this.nextActivation = builder.nextActivation;
    this.processing = builder.processing;
    this.requestData = builder.requestData;
    this.stateVariables = builder.stateVariables;
    this.actions = builder.actions;
    this.retries = builder.retries;
    this.created = builder.created;
    this.modified = builder.modified;
    this.owner = builder.owner;
  }

  public static class Builder {

    Integer id;
    String type;
    String businessKey;
    String state;
    String stateText;
    DateTime nextActivation;
    boolean processing;
    String requestData;
    Map<String, String> stateVariables;
    List<WorkflowInstanceAction> actions;
    int retries;
    DateTime created;
    DateTime modified;
    String owner;

    public Builder() {
    }

    public Builder(WorkflowInstance copy) {
      this.id = copy.id;
      this.type = copy.type;
      this.businessKey = copy.businessKey;
      this.state = copy.state;
      this.stateText = copy.stateText;
      this.nextActivation = copy.nextActivation;
      this.processing = copy.processing;
      this.requestData = copy.requestData;
      this.stateVariables = copy.stateVariables;
      this.retries = copy.retries;
      this.created = copy.created;
      this.modified = copy.modified;
      this.owner = copy.owner;
    }

    public Builder setId(Integer id) {
      this.id = id;
      return this;
    }

    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    public Builder setBusinessKey(String businessKey) {
      this.businessKey = businessKey;
      return this;
    }

    public Builder setState(String state) {
      this.state = state;
      return this;
    }

    public Builder setStateText(String stateText) {
      this.stateText = stateText;
      return this;
   }

    public Builder setNextActivation(DateTime nextActivation) {
      this.nextActivation = nextActivation;
      return this;
    }

    public Builder setProcessing(boolean processing) {
      this.processing = processing;
      return this;
    }

    public Builder setRequestData(String requestData) {
      this.requestData = requestData;
      return this;
    }

    public Builder setStateVariables(Map<String, String> stateVariables) {
      this.stateVariables = stateVariables;
      return this;
    }

    public Builder setActions(ArrayList<WorkflowInstanceAction> actions) {
      this.actions = actions;
      return this;
    }

    public Builder setRetries(int retries) {
      this.retries = retries;
      return this;
    }

    public Builder setCreated(DateTime created) {
      this.created = created;
      return this;
    }

    public Builder setModified(DateTime modified) {
      this.modified = modified;
      return this;
    }

    public Builder setOwner(String owner) {
      this.owner = owner;
      return this;
    }

    public WorkflowInstance build() {
      return new WorkflowInstance(this);
    }

  }

}
