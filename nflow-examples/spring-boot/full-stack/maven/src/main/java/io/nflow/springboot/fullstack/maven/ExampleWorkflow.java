package io.nflow.springboot.fullstack.maven;

import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static org.joda.time.DateTime.now;

import io.nflow.engine.workflow.curated.State;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowStateType;

public class ExampleWorkflow extends WorkflowDefinition {

  public static final String TYPE = "repeatingWorkflow";
  public static final String VAR_COUNTER = "VAR_COUNTER";
  private static final State REPEAT = new State("repeat", WorkflowStateType.start, "Repeating state");
  private static final State ERROR = new State("error", WorkflowStateType.manual, "Error state");

  public ExampleWorkflow() {
    super(TYPE, REPEAT, ERROR);
    permit(REPEAT, REPEAT);
  }

  public NextAction repeat(StateExecution execution) {
    System.out.println("Counter: " + execution.getVariable(VAR_COUNTER));
    execution.setVariable(VAR_COUNTER, execution.getVariable(VAR_COUNTER, Integer.class) + 1);
    return moveToStateAfter(REPEAT, now().plusSeconds(10), "Next iteration");
  }
}
