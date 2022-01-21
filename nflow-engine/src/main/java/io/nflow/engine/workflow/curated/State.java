package io.nflow.engine.workflow.curated;

import io.nflow.engine.model.ModelObject;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

/**
 * A simple implementation of the WorkflowState interface that can be used in any workflow definition, if more advanced
 * implementation is not needed.
 */
public class State extends ModelObject implements WorkflowState {

  private final String name;
  private final WorkflowStateType type;
  private final String description;

  /**
   * Creates a workflow state with given name and type <code>normal</code>. The name is also used as the description of the state.
   *
   * @param name
   *          The name of the state.
   */
  public State(String name) {
    this(name, WorkflowStateType.normal, name);
  }

  /**
   * Creates a workflow state with given name and type. The name is also used as the description of the state.
   *
   * @param name
   *          The name of the state.
   * @param type
   *          The workflow state type.
   */
  public State(String name, WorkflowStateType type) {
    this(name, type, name);
  }

  /**
   * Creates a workflow state with given name and description and type <code>normal</code>.
   *
   * @param name
   *          The name of the state.
   * @param description
   *          The longer description of the state.
   */
  public State(String name, String description) {
    this(name, WorkflowStateType.normal, description);
  }

  /**
   * Creates a workflow state with given name, description and type.
   *
   * @param name
   *          The name of the state.
   * @param type
   *          The workflow state type.
   * @param description
   *          The longer description of the state.
   */
  public State(String name, WorkflowStateType type, String description) {
    this.name = name;
    this.type = type;
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
