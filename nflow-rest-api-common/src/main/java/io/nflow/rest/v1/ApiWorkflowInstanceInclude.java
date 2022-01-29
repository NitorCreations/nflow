package io.nflow.rest.v1;

import static java.util.Arrays.stream;

import io.nflow.engine.service.WorkflowInstanceInclude;

/**
 * The properties that can be loaded if needed when fetching worklfow instances from the nFlow REST API.
 */
public enum ApiWorkflowInstanceInclude {

  /**
   * {@link WorkflowInstanceInclude#CURRENT_STATE_VARIABLES}
   */
  currentStateVariables(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES),

  /**
   * {@link WorkflowInstanceInclude#CHILD_WORKFLOW_IDS}
   */
  childWorkflows(WorkflowInstanceInclude.CHILD_WORKFLOW_IDS),

  /**
   * {@link WorkflowInstanceInclude#ACTIONS}
   */
  actions(WorkflowInstanceInclude.ACTIONS),

  /**
   * {@link WorkflowInstanceInclude#ACTION_STATE_VARIABLES}
   */
  actionStateVariables(WorkflowInstanceInclude.ACTION_STATE_VARIABLES);

  private WorkflowInstanceInclude include;

  ApiWorkflowInstanceInclude(WorkflowInstanceInclude include) {
    this.include = include;
  }

  /**
   * Provide mapping to {@link WorkflowInstanceInclude}.
   *
   * @return Corresponding {@link WorkflowInstanceInclude} value.
   */
  public WorkflowInstanceInclude getInclude() {
    return include;
  }

  /**
   * Resolve enum value from string.
   *
   * @return Matching enum value, or null if no match found.
   */
  public static ApiWorkflowInstanceInclude fromValue(String value) {
    return stream(values()).filter(include -> include.name().equals(value)).findAny().orElse(null);
  }
}
