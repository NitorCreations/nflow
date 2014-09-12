package com.nitorcreations.nflow.engine.workflow.instance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;

/**
 * An instance of a workflow.
 */
public class WorkflowInstance {

  public final Integer id;
  public final String type;
  public final String businessKey;
  public final String externalId;
  public final String state;
  public final String stateText;
  public final DateTime nextActivation;
  public final boolean processing;
  public final Map<String, String> stateVariables;
  public final Map<String, String> originalStateVariables;
  public final List<WorkflowInstanceAction> actions;
  public final int retries;
  public final DateTime created;
  public final DateTime modified;
  public final String owner;

  WorkflowInstance(Builder builder) {
    this.id = builder.id;
    this.type = builder.type;
    this.businessKey = builder.businessKey;
    this.externalId = builder.externalId;
    this.state = builder.state;
    this.stateText = builder.stateText;
    this.nextActivation = builder.nextActivation;
    this.processing = builder.processing;
    this.originalStateVariables = builder.originalStateVariables;
    this.stateVariables = builder.stateVariables;
    this.actions = builder.actions;
    this.retries = builder.retries;
    this.created = builder.created;
    this.modified = builder.modified;
    this.owner = builder.owner;
  }

  /**
   * Builder for workflow instance.
   */
  public static class Builder {

    Integer id;
    String type;
    String businessKey;
    String externalId;
    String state;
    String stateText;
    DateTime nextActivation;
    boolean processing;
    final Map<String, String> originalStateVariables = new LinkedHashMap<>();
    final Map<String, String> stateVariables = new LinkedHashMap<>();
    List<WorkflowInstanceAction> actions;
    int retries;
    DateTime created;
    DateTime modified;
    String owner;

    private ObjectStringMapper mapper;

    /**
     * Create a workflow instance builder.
     */
    public Builder() {
    }

    /**
     * Create a workflow instance builder with an object mapper.
     * @param objectMapper The object mapper.
     */
    public Builder(ObjectStringMapper objectMapper) {
      this.mapper = objectMapper;
    }

    /**
     * Create a workflow instance builder based on an existing workflow instance.
     * @param copy The instance to be used as a basis for the new instance.
     */
    public Builder(WorkflowInstance copy) {
      this.id = copy.id;
      this.type = copy.type;
      this.businessKey = copy.businessKey;
      this.externalId = copy.externalId;
      this.state = copy.state;
      this.stateText = copy.stateText;
      this.nextActivation = copy.nextActivation;
      this.processing = copy.processing;
      this.originalStateVariables.putAll(copy.originalStateVariables);
      this.stateVariables.putAll(copy.stateVariables);
      this.retries = copy.retries;
      this.created = copy.created;
      this.modified = copy.modified;
      this.owner = copy.owner;
    }

    /**
     * Set the workflow instance identifier.
     * @param id The identifier.
     * @return this.
     */
    public Builder setId(Integer id) {
      this.id = id;
      return this;
    }

    /**
     * Set the type of the workflow definition.
     * @param type The type.
     * @return this.
     */
    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    /**
     * Set the business key.
     * @param businessKey The business key.
     * @return this.
     */
    public Builder setBusinessKey(String businessKey) {
      this.businessKey = businessKey;
      return this;
    }

    /**
     * Set the external identifier.
     * @param externalId The external identifier.
     * @return this.
     */
    public Builder setExternalId(String externalId) {
      this.externalId = externalId;
      return this;
    }

    /**
     * Set the state.
     * @param state The name of the state.
     * @return this.
     */
    public Builder setState(String state) {
      this.state = state;
      return this;
    }

    /**
     * Set the state text.
     * @param stateText The state text.
     * @return this.
     */
    public Builder setStateText(String stateText) {
      this.stateText = stateText;
      return this;
   }

    /**
     * Set the next activation time.
     * @param nextActivation The next activation time.
     * @return this.
     */
    public Builder setNextActivation(DateTime nextActivation) {
      this.nextActivation = nextActivation;
      return this;
    }

    /**
     * Set the flag that indicates whether a thread is processing this workflow or not.
     * @param processing True if the workflow is being processed, false otherwise.
     * @return this.
     */
    public Builder setProcessing(boolean processing) {
      this.processing = processing;
      return this;
    }

    /**
     * Set the original state variables.
     * @param originalStateVariables The original state variables.
     * @return this.
     */
    public Builder setOriginalStateVariables(Map<String, String> originalStateVariables) {
      this.originalStateVariables.clear();
      this.originalStateVariables.putAll(originalStateVariables);
      return this;
    }

    /**
     * Set the state variables.
     * @param stateVariables The state variables.
     * @return this.
     */
    public Builder setStateVariables(Map<String, String> stateVariables) {
      this.stateVariables.clear();
      this.stateVariables.putAll(stateVariables);
      return this;
    }

    /**
     * Put a state variable to the state variables map.
     * @param key The name of the variable.
     * @param value The string value of the variable.
     * @return this.
     */
    public Builder putStateVariable(String key, String value) {
      this.stateVariables.put(key, value);
      return this;
    }

    /**
     * Put a state variable to the state variables map.
     * @param key The name of the variable.
     * @param value The value of the variable, serialized by object mapper.
     * @return this.
     */
    public Builder putStateVariable(String key, Object value) {
      if (mapper == null) {
        throw new IllegalStateException("WorkflowInstance.Builder must be created using WorkflowInstanceFactory.newWorkflowInstanceBuilder()");
      }
      this.stateVariables.put(key, mapper.convertFromObject(key, value));
      return this;
    }

    /**
     * Set the workflow instance actions.
     * @param actions List of actions.
     * @return this.
     */
    public Builder setActions(List<WorkflowInstanceAction> actions) {
      this.actions = actions;
      return this;
    }

    /**
     * Set the number of retries.
     * @param retries The number of retries.
     * @return this.
     */
    public Builder setRetries(int retries) {
      this.retries = retries;
      return this;
    }

    /**
     * Set the creation timestamp.
     * @param created Creation time.
     * @return this.
     */
    public Builder setCreated(DateTime created) {
      this.created = created;
      return this;
    }

    /**
     * Set the modification timestamp.
     * @param modified Modification time.
     * @return this.
     */
    public Builder setModified(DateTime modified) {
      this.modified = modified;
      return this;
    }

    /**
     * Set the executor group name.
     * @param owner The executor group name.
     * @return this.
     */
    public Builder setOwner(String owner) {
      this.owner = owner;
      return this;
    }

    /**
     * Create the workflow instance object.
     * @return The workflow instance.
     */
    public WorkflowInstance build() {
      return new WorkflowInstance(this);
    }
  }
}
