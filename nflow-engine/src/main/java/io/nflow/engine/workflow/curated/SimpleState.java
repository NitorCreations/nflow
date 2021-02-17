package io.nflow.engine.workflow.curated;

import io.nflow.engine.model.ModelObject;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

public class SimpleState extends ModelObject implements WorkflowState {

  private final String name;
  private final WorkflowStateType type;
  private final String description;

  public SimpleState(String name) {
    this(name, WorkflowStateType.normal, name);
  }

  public SimpleState(String name, WorkflowStateType type) {
    this(name, type, name);
  }

  public SimpleState(String name, String description) {
    this(name, WorkflowStateType.normal, description);
  }

  public SimpleState(String name, WorkflowStateType type, String description) {
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
