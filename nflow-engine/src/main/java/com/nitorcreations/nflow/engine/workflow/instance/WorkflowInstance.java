package com.nitorcreations.nflow.engine.workflow.instance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateTime;

import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;

/**
 * An instance of a workflow.
 */
public class WorkflowInstance {

  /**
   * Describes the status for workflow instance.
   */
  public static enum WorkflowInstanceStatus {
    /** Workflow instance has been created but the processing has not been started yet */
    created,
    /** Workflow instance processing has been started */
    inProgress,
    /** Workflow instance has reached an end state */
    finished,
    /** Workflow instance is waiting for manual action */
    manual,
    /** A workflow state method is executing */
    executing
  }

  /**
   * The workflow instance identifier.
   */
  public final Integer id;

  /**
   * The id of executor that is currently processing this workflow. May be null.
   */
  public final Integer executorId;

  /**
   * The id of the workflow that created the hierarchy of workflow where this sub workflow belongs to.
   * Null for workflows that are the root of hierarchy.
   */
  public final Integer rootWorkflowId;

  /**
   * The id of the workflow that created this sub workflow. Is null for root workflows.
   */
  public final Integer parentWorkflowId;

  /**
   * The id of the workflow action that created this sub workflow.  Is null for root workflows.
   */
  public final Integer parentActionId;

  /**
   * The current status of the workflow instance.
   */
  public final WorkflowInstanceStatus status;

  /**
   * The type of the workflow definition.
   */
  public final String type;

  /**
   * Business key.
   */
  public final String businessKey;

  /**
   * External identifier of the workflow instance. Must be unique within the same executor group.
   */
  public final String externalId;

  /**
   * The name of the current state.
   */
  public final String state;

  /**
   * The description of the last action executed by the workflow.
   */
  public final String stateText;

  /**
   * The next activation time for the workflow instance.
   */
  public final DateTime nextActivation;

  /**
   * The state variables. Uses the variable name as the key and serialized variable value as value.
   */
  public final Map<String, String> stateVariables;

  /**
   * The state variable values before executor started processing the state.
   */
  public final Map<String, String> originalStateVariables;

  /**
   * The list of actions.
   */
  public final List<WorkflowInstanceAction> actions;

  /**
   * Number of retry attempts of the current state. Zero when the state is executed for the first time.
   */
  public final int retries;

  /**
   * The workflow instance creation time.
   */
  public final DateTime created;

  /**
   * The last modification time of the workflow instance.
   */
  public final DateTime modified;

  /**
   * Time when workflow processing was started, that is, time when processing of first
   * action started.
   */
  public final DateTime started;

  /**
   * The name of the executor group for this workflow instance.
   */
  public final String executorGroup;

  /**
   * Child workflow instance IDs created by this workflow instance, grouped by instance action ID.
   */
  public Map<Integer, List<Integer>> childWorkflows;

  WorkflowInstance(Builder builder) {
    this.id = builder.id;
    this.executorId = builder.executorId;
    this.rootWorkflowId = builder.rootWorkflowId;
    this.parentWorkflowId = builder.parentWorkflowId;
    this.parentActionId = builder.parentActionId;
    this.status = builder.status;
    this.type = builder.type;
    this.businessKey = builder.businessKey;
    this.externalId = builder.externalId;
    this.state = builder.state;
    this.stateText = builder.stateText;
    this.nextActivation = builder.nextActivation;
    this.originalStateVariables = builder.originalStateVariables;
    this.stateVariables = builder.stateVariables;
    this.actions = builder.actions;
    this.childWorkflows = builder.childWorkflows;
    this.retries = builder.retries;
    this.created = builder.created;
    this.modified = builder.modified;
    this.started = builder.started;
    this.executorGroup = builder.executorGroup;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }

  /**
   * Builder for workflow instance.
   */
  public static class Builder {

    Integer id;
    Integer executorId;
    Integer rootWorkflowId;
    Integer parentWorkflowId;
    Integer parentActionId;
    WorkflowInstanceStatus status;
    String type;
    String businessKey;
    String externalId;
    String state;
    String stateText;
    DateTime nextActivation;
    final Map<String, String> originalStateVariables = new LinkedHashMap<>();
    final Map<String, String> stateVariables = new LinkedHashMap<>();
    List<WorkflowInstanceAction> actions = new ArrayList<>();
    final Map<Integer, List<Integer>> childWorkflows = new LinkedHashMap<>();
    int retries;
    DateTime created;
    DateTime started;
    DateTime modified;
    String executorGroup;

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
      this.executorId = copy.executorId;
      this.rootWorkflowId = copy.rootWorkflowId;
      this.parentWorkflowId = copy.parentWorkflowId;
      this.parentActionId = copy.parentActionId;
      this.status = copy.status;
      this.type = copy.type;
      this.businessKey = copy.businessKey;
      this.externalId = copy.externalId;
      this.state = copy.state;
      this.stateText = copy.stateText;
      this.nextActivation = copy.nextActivation;
      this.originalStateVariables.putAll(copy.originalStateVariables);
      this.stateVariables.putAll(copy.stateVariables);
      this.actions.addAll(copy.actions);
      this.childWorkflows.putAll(copy.childWorkflows);
      this.retries = copy.retries;
      this.created = copy.created;
      this.modified = copy.modified;
      this.executorGroup = copy.executorGroup;
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
     * Set the executor instance identifier.
     * @param executorId The identifier.
     * @return this.
     */
    public Builder setExecutorId(Integer executorId) {
      this.executorId = executorId;
      return this;
    }

    /**
     * Set the root workflow identifier.
     * @param rootWorkflowId The identifier.
     * @return this
     */
    public Builder setRootWorkflowId(Integer rootWorkflowId) {
      this.rootWorkflowId = rootWorkflowId;
      return this;
    }

    /**
     * Set the parent workflow identifier.
     * @param parentWorkflowId The identifier.
     * @return this.
     */
    public Builder setParentWorkflowId(Integer parentWorkflowId) {
      this.parentWorkflowId = parentWorkflowId;
      return this;
    }

    /**
     * Set the parent workflow identifier.
     * @param parentActionId The identifier.
     * @return this.
     */
    public Builder setParentActionId(Integer parentActionId) {
      this.parentActionId = parentActionId;
      return this;
    }

    /**
     * Set the status.
     * @param status The status.
     * @return this.
     */
    public Builder setStatus(WorkflowInstanceStatus status) {
      this.status = status;
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
     * Set the start timestamp.
     * @param started Start time.
     * @return this.
     */
    public Builder setStarted(DateTime started) {
      this.started = started;
      return this;
    }

    /**
     * Set the executor group name.
     * @param executorGroup The executor group name.
     * @return this.
     */
    public Builder setExecutorGroup(String executorGroup) {
      this.executorGroup = executorGroup;
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
