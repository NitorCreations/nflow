package com.nitorcreations.nflow.engine.domain;

import org.joda.time.DateTime;

public class WorkflowInstance {

  public final Integer id;
  public final String type;
  public final String businessKey;
  public final String state;
  public final String stateText;
  public final DateTime nextActivation;
  public final boolean currentlyProcessing;
  public final String requestData;     
  public final String stateVariables;  // TODO: change to map
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
    this.currentlyProcessing = builder.currentlyProcessing;
    this.requestData = builder.requestData;
    this.stateVariables = builder.stateVariables;
    this.created = builder.created;
    this.modified = builder.modified;
    this.owner = builder.owner;
  }
  
  public String getRequestData() {
    return requestData;
  }

  public static class Builder {   
    
    Integer id;
    String type;
    String businessKey;
    String state;
    String stateText;
    DateTime nextActivation;
    boolean currentlyProcessing;
    String requestData;
    String stateVariables;
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
      this.currentlyProcessing = copy.currentlyProcessing;
      this.requestData = copy.requestData;
      this.stateVariables =copy.stateVariables;
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

    public Builder setCurrentlyProcessing(boolean currentlyProcessing) {
      this.currentlyProcessing = currentlyProcessing;
      return this;
    }

    public Builder setRequestData(String requestData) {
      this.requestData = requestData;
      return this;
    }

    public Builder setStateVariables(String stateVariables) {
      this.stateVariables = stateVariables;
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
