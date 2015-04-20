package com.nitorcreations.nflow.engine.workflow.definition;

import java.util.HashSet;
import java.util.Set;

/**
 * The base class for dynamic workflow definitions.
 */
public class DynamicWorkflowDefinition extends AbstractWorkflowDefinition<WorkflowState> {
  private final Set<WorkflowState> allStates;

  DynamicWorkflowDefinition(WorkflowDefinitionBuilder builder) {
    super(builder.type, builder.initialState, builder.errorState, builder.settings.build(), builder.methods);
    this.allStates = new HashSet<>(builder.states);
  }

  /**
   * Return all states of the workflow.
   * @return Set of workflow states.
   */
  @Override
  public Set<WorkflowState> getStates() {
    return allStates;
  }

  static class GenericMethodState implements WorkflowState {
    private final WorkflowStateType type;
    private final String name;
    private final String description;

    public GenericMethodState(WorkflowStateType type, String name, String description) {
      this.type = type;
      this.name = name;
      this.description = description;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return description;
    }
  }
}
