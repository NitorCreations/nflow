package com.nitorcreations.nflow.demo;

import static com.nitorcreations.nflow.engine.workflow.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.WorkflowStateType.manual;
import static com.nitorcreations.nflow.engine.workflow.WorkflowStateType.normal;
import static org.slf4j.LoggerFactory.getLogger;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.WorkflowStateType;

public class DemoWorkflow extends WorkflowDefinition<DemoWorkflow.State> {

  private static final Logger logger = getLogger(DemoWorkflow.class);

  public static enum State implements WorkflowState {
    start(WorkflowStateType.start), process(normal), done(end), error(manual);

    private WorkflowStateType type;

    private State(WorkflowStateType type) {
      this.type = type;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getName() {
	return name();
    }

    @Override
    public String getDescription() {
	return name();
    }
  }

  public DemoWorkflow() {
    super("demo", State.start, State.error);
    permit(State.start, State.process);
    permit(State.process, State.done);
  }

  public void start(StateExecution execution) {
    execution.setNextState(State.process);
    execution.setNextActivation(DateTime.now());
  }

  public void process(StateExecution execution) {
    execution.setNextState(State.done);
    execution.setNextActivation(DateTime.now());
  }

  public void done(StateExecution execution) {
    execution.setNextState(State.done);
    logger.info("Finished.");
  }

}
