package io.nflow.engine.workflow.instance;

import static java.util.Collections.emptyMap;
import static org.joda.time.DateTime.now;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.joda.time.DateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.model.ModelObject;

/**
 * An instance of a workflow.
 */
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "used by nflow-rest")
public class WorkflowInstance extends ModelObject {

  /**
   * Describes the status for workflow instance.
   */
  public enum WorkflowInstanceStatus {
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
   * The signal raised for this workflow instance.
   */
  public final Optional<Integer> signal;

  /**
   * Child workflow instance IDs created by this workflow instance, grouped by instance action ID.
   */
  public Map<Integer, List<Integer>> childWorkflows;

  ObjectStringMapper mapper;

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
    this.signal = builder.signal;
    this.mapper = builder.mapper;
  }

  /**
   * Return the state variables that have been added or modified during state processing.
   * @return New and modified state variables.
   */
  public Map<String, String> getChangedStateVariables() {
    if (stateVariables == null) {
      return emptyMap();
    }
    Map<String, String> changedVariables = new HashMap<>(stateVariables.size());
    for (Entry<String, String> current : stateVariables.entrySet()) {
      String oldVal = originalStateVariables.get(current.getKey());
      if (oldVal == null || !oldVal.equals(current.getValue())) {
        changedVariables.put(current.getKey(), current.getValue());
      }
    }
    return changedVariables;
  }

  public String getStateVariable(String name) {
    return getStateVariable(name, (String) null);
  }

  public <T> T getStateVariable(String name, Class<T> valueType) {
    return getStateVariable(name, valueType, null);
  }

  @SuppressWarnings("unchecked")
  public <T> T getStateVariable(String name, Class<T> valueType, T defaultValue) {
    if (mapper == null) {
      throw new IllegalStateException(
          "WorkflowInstance.Builder must be created using WorkflowInstanceFactory.newWorkflowInstanceBuilder()");
    }
    if (stateVariables.containsKey(name)) {
      return (T) mapper.convertToObject(valueType, name, stateVariables.get(name));
    }
    return defaultValue;
  }

  public String getStateVariable(String name, String defaultValue) {
    if (stateVariables.containsKey(name)) {
      return stateVariables.get(name);
    }
    return defaultValue;
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
    DateTime nextActivation = now();
    final Map<String, String> originalStateVariables = new LinkedHashMap<>();
    final Map<String, String> stateVariables = new LinkedHashMap<>();
    List<WorkflowInstanceAction> actions = new ArrayList<>();
    final Map<Integer, List<Integer>> childWorkflows = new LinkedHashMap<>();
    int retries;
    DateTime created;
    DateTime started;
    DateTime modified;
    String executorGroup;
    Optional<Integer> signal = Optional.empty();
    ObjectStringMapper mapper;

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
      this.signal = copy.signal;
      this.mapper = copy.mapper;
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
      assertNotNull(value, "State variable " + key + " value cannot be null");
      this.stateVariables.put(key, value);
      return this;
    }

    /**
     * Put a state variable to the state variables map.
     * @param key The name of the variable.
     * @param value The value of the variable, serialized by object mapper.
     * @return this.
     */
    @SuppressFBWarnings(value = "WEM_WEAK_EXCEPTION_MESSAGING", justification = "exception message is ok")
    public Builder putStateVariable(String key, Object value) {
      if (mapper == null) {
        throw new IllegalStateException("WorkflowInstance.Builder must be created using WorkflowInstanceFactory.newWorkflowInstanceBuilder()");
      }
      assertNotNull(value, "State variable " + key + " value cannot be null");
      this.stateVariables.put(key, mapper.convertFromObject(key, value));
      return this;
    }

    /**
     * Put a state variable to the state variables map if the optional value is present. If the optionalValue is empty, existing
     * state variable value is not changed.
     * @param key The name of the variable.
     * @param optionalValue The optional value of the variable, serialized by object mapper.
     * @return this.
     */
    @SuppressFBWarnings(value = "WEM_WEAK_EXCEPTION_MESSAGING", justification = "exception message is ok")
    public Builder putStateVariable(String key, Optional<?> optionalValue) {
      return optionalValue.map(value -> putStateVariable(key, value)).orElse(this);
    }

    private void assertNotNull(Object value, String reason) {
      if (value == null) {
        throw new IllegalArgumentException(reason);
      }
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
     * Set the signal value.
     * @param signal The signal value.
     * @return this.
     */
    public Builder setSignal(Optional<Integer> signal) {
      this.signal = signal;
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
