package com.nitorcreations.nflow.engine.workflow.instance;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

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
   * Workflow instance states.
   */
  public final List<String> states;

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

  QueryWorkflowInstances(Builder builder) {
    super();
    this.ids = new ArrayList<>(builder.ids);
    this.types = new ArrayList<>(builder.types);
    this.states = new ArrayList<>(builder.states);
    this.businessKey = builder.businessKey;
    this.externalId = builder.externalId;
    this.includeActions = builder.includeActions;
    this.includeCurrentStateVariables = builder.includeCurrentStateVariables;
    this.includeActionStateVariables = builder.includeActionStateVariables;
  }

  /**
   * Builder for workflow instance queries.
   */
  public static class Builder {
    List<Integer> ids = new ArrayList<>();
    List<String> types = new ArrayList<>();
    List<String> states = new ArrayList<>();
    String businessKey;
    String externalId;
    boolean includeActions;
    boolean includeCurrentStateVariables;
    boolean includeActionStateVariables;

    /**
     * Create a workflow instance query builder.
     */
    public Builder() {
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
     * Add workflow states to query parameters.
     * @param newStates The state names.
     * @return this.
     */
    public Builder addStates(String ... newStates) {
      this.states.addAll(asList(newStates));
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
     * @param includeActions True to include state variables, false otherwise.
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
     * Create the workflow instance query object.
     * @return Workflow instance query.
     */
    public QueryWorkflowInstances build() {
      return new QueryWorkflowInstances(this);
    }
  }
}
