package io.nflow.springboot.bareminimum;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowStateType;

import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static io.nflow.springboot.bareminimum.ExampleWorkflow.State.error;
import static io.nflow.springboot.bareminimum.ExampleWorkflow.State.repeat;
import static org.joda.time.DateTime.now;

public class ExampleWorkflow extends WorkflowDefinition<ExampleWorkflow.State> {

  public static final String TYPE = "repeatingWorkflow";
  public static final String VAR_COUNTER = "VAR_COUNTER";

  public enum State implements io.nflow.engine.workflow.definition.WorkflowState {
    repeat(start, "Repeating state"),
    error(manual, "Error state");

    private WorkflowStateType type;
    private String description;

    State(WorkflowStateType type, String description) {
      this.type = type;
      this.description = description;
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

  public ExampleWorkflow() {
    super(TYPE, repeat, error);
    permit(repeat, repeat);
  }

  public NextAction repeat(StateExecution execution) {
    System.out.println("Counter: " + execution.getVariable(VAR_COUNTER));
    execution.setVariable(VAR_COUNTER, execution.getVariable(VAR_COUNTER, Integer.class) + 1);
    return moveToStateAfter(repeat, now().plusSeconds(10), "Next iteration");
  }
}
