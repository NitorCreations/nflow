package com.nitorcreations.nflow.engine.workflow.instance;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

/**
 * Parameters for workflow instance query.
 */
public class QueryWorkflowInstances {

  /**
   * Workflow instance identifiers.
   */
  public final List<Integer> ids;

  /**
   * Workflow instance definition type.
   */
  public final List<String> types;

  /**
   * Parent workflow instance id.
   */
  public Integer parentWorkflowId;

  /**
   * Parent workflow action id.
   */
  public Integer parentActionId;

  /**
   * Workflow instance states.
   */
  public final List<String> states;

  /**
   * Workflow instance statuses.
   */
  public final List<WorkflowInstanceStatus> statuses;

  /**
   * External business key.
   */
  public final String businessKey;

  /**
   * External identifier.
   */
  public final String externalId;

  /**
   * Setting this to true will make the query return also workflow actions.
   */
  public final boolean includeActions;

  /**
   * Setting this to true will make the query return also the current state variables for the workflow.
   */
  public final boolean includeCurrentStateVariables;

  /**
   * Setting this to true will make the query return also the updated state variables for workflow actions.
   */
  public final boolean includeActionStateVariables;

  /**
   * Setting this to true will make the query return also the created child workflow instance IDs.
   */
  public final boolean includeChildWorkflows;

  /**
   * The maximum number of instances to be returned by the query. If null, uses default value configured for the nFlow engine. The
   * maximum value may also be limited by nFlow engine configuration.
   */
  public final Long maxResults;

  QueryWorkflowInstances(Builder builder) {
    this.ids = new ArrayList<>(builder.ids);
    this.types = new ArrayList<>(builder.types);
    this.parentWorkflowId = builder.parentWorkflowId;
    this.parentActionId = builder.parentActionId;
    this.states = new ArrayList<>(builder.states);
    this.statuses = new ArrayList<>(builder.statuses);
    this.businessKey = builder.businessKey;
    this.externalId = builder.externalId;
    this.includeActions = builder.includeActions;
    this.includeCurrentStateVariables = builder.includeCurrentStateVariables;
    this.includeActionStateVariables = builder.includeActionStateVariables;
    this.includeChildWorkflows = builder.includeChildWorkflows;
    this.maxResults = builder.maxResults;
  }

  /**
   * Builder for workflow instance queries.
   */
  public static class Builder {
    List<Integer> ids = new ArrayList<>();
    List<String> types = new ArrayList<>();
    Integer parentWorkflowId;
    Integer parentActionId;
    List<String> states = new ArrayList<>();
    List<WorkflowInstanceStatus> statuses = new ArrayList<>();
    String businessKey;
    String externalId;
    boolean includeActions;
    boolean includeCurrentStateVariables;
    boolean includeActionStateVariables;
    boolean includeChildWorkflows;
    Long maxResults;

    /**
     * Create a workflow instance query builder.
     */
    public Builder() {
    }

    public Builder(QueryWorkflowInstances copy) {
      this.ids = copy.ids;
      this.types = copy.types;
      this.parentWorkflowId = copy.parentWorkflowId;
      this.parentActionId = copy.parentActionId;
      this.states = copy.states;
      this.statuses = copy.statuses;
      this.businessKey = copy.businessKey;
      this.externalId = copy.externalId;
      this.includeActions = copy.includeActions;
      this.includeCurrentStateVariables = copy.includeCurrentStateVariables;
      this.includeActionStateVariables = copy.includeActionStateVariables;
      this.includeChildWorkflows = copy.includeChildWorkflows;
      this.maxResults = copy.maxResults;
    }
    /**
     * Add identifiers to query parameters.
     * @param newIds The identifiers.
     * @return this.
     */
    public Builder addIds(Integer ... newIds) {
      this.ids.addAll(asList(newIds));
      return this;
    }

    /**
     * Add workflow definitions types to query parameters.
     * @param newTypes The types.
     * @return this.
     */
    public Builder addTypes(String ... newTypes) {
      this.types.addAll(asList(newTypes));
      return this;
    }

    /**
     * Set parent workflow instance id to query parameters.
     * @param parentWorkflowId The parent workflow instance id.
     * @return this.
     */
    public Builder setParentWorkflowId(Integer parentWorkflowId) {
      this.parentWorkflowId = parentWorkflowId;
      return this;
    }

    /**
     * Set parent action id to query parameters.
     * @param parentActionId The parent action id.
     * @return this.
     */
    public Builder setParentActionId(Integer parentActionId) {
      this.parentActionId = parentActionId;
      return this;
    }

    /**
     * Add workflow states to query parameters.
     * @param newStates The state names.
     * @return this.
     */
    public Builder addStates(String ... newStates) {
      this.states.addAll(asList(newStates));
      return this;
    }

    /**
     * Add workflow statuses to query parameters.
     * @param newStatuses The statuses.
     * @return this.
     */
    public Builder addStatuses(WorkflowInstanceStatus... newStatuses) {
      this.statuses.addAll(asList(newStatuses));
      return this;
    }

    /**
     * Set business key to query parameters.
     * @param businessKey The business key.
     * @return this.
     */
    public Builder setBusinessKey(String businessKey) {
      this.businessKey = businessKey;
      return this;
    }

    /**
     * Set external identifier to query parameters.
     * @param externalId The external identifier.
     * @return this.
     */
    public Builder setExternalId(String externalId) {
      this.externalId = externalId;
      return this;
    }

    /**
     * Set whether workflow actions should be included in the results.
     * @param includeActions True to include actions, false otherwise.
     * @return this.
     */
    public Builder setIncludeActions(boolean includeActions) {
      this.includeActions = includeActions;
      return this;
    }

    /**
     * Set whether current workflow state variables should be included in the results.
     * @param includeCurrentStateVariables True to include state variables, false otherwise.
     * @return this.
     */
    public Builder setIncludeCurrentStateVariables(boolean includeCurrentStateVariables) {
      this.includeCurrentStateVariables = includeCurrentStateVariables;
      return this;
    }

    /**
     * Set whether state variables for workflow actions should be included in the results.
     * @param includeActionStateVariables True to include state variables, false otherwise.
     * @return this.
     */
    public Builder setIncludeActionStateVariables(boolean includeActionStateVariables) {
      this.includeActionStateVariables = includeActionStateVariables;
      return this;
    }

    /**
     * Set whether child workflow IDs created by this instance should be included in the results.
     * @param includeChildWorkflows True to include child workflows, false otherwise.
     * @return this.
     */
    public Builder setIncludeChildWorkflows(boolean includeChildWorkflows) {
      this.includeChildWorkflows = includeChildWorkflows;
      return this;
    }

    /**
     * Set the maximum number of instances to be returned.
     * @param maxResults The maximum number of instances to be returned.
     * @return this.
     */
    public Builder setMaxResults(Long maxResults) {
      this.maxResults = maxResults;
      return this;
    }

    /**
     * Create the workflow instance query object.
     * @return Workflow instance query.
     */
    public QueryWorkflowInstances build() {
      return new QueryWorkflowInstances(this);
    }
  }
}
