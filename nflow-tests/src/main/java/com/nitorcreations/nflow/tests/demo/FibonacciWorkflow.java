package com.nitorcreations.nflow.tests.demo;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToState;
import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.retryAfter;
import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.stopInState;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.start;
import static java.lang.Integer.parseInt;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.slf4j.Logger;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.StateVar;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

/**
 * Fibonacci series generator using recursive process. Each step is handled by a new child workflow.
 */
public class FibonacciWorkflow extends WorkflowDefinition<FibonacciWorkflow.State> {
  public static final String WORKFLOW_TYPE = "fibonacci";
  private static final Logger logger = getLogger(FibonacciWorkflow.class);

  public enum State implements WorkflowState {
    begin(start), nMinus1(normal), nMinus2(normal), poll(normal), done(end), error(manual);

    private WorkflowStateType type;

    State(WorkflowStateType type) {
      this.type = type;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return name();
    }
  }

  public FibonacciWorkflow() {
    super(WORKFLOW_TYPE, State.begin, State.error);
    permit(State.begin, State.nMinus2);
    permit(State.nMinus2, State.nMinus1);
    permit(State.nMinus2, State.poll);
    permit(State.nMinus1, State.poll);
    permit(State.nMinus1, State.done);
    permit(State.nMinus2, State.done);
    permit(State.poll, State.done);
  }

  public NextAction begin(StateExecution execution, @StateVar(value = "requestData", readOnly = true) FiboData fiboData) {
    int n = fiboData.n;
    logger.info("Fibonacci step N = {}", n);
    execution.setVariable("result", 0);
    return moveToState(State.nMinus2, "Starting N = " + n);
  }

  public NextAction nMinus2(StateExecution execution, @StateVar(value = "requestData", readOnly = true) FiboData fiboData) {
    int n = fiboData.n;
    return nextStep(execution, n - 2, 2, State.nMinus1);
  }

  public NextAction nMinus1(StateExecution execution, @StateVar(value = "requestData", readOnly = true) FiboData fiboData) {
    int n = fiboData.n;
    return nextStep(execution, n - 1, 1, State.poll);
  }

  private NextAction nextStep(StateExecution execution, int nextN, int offset, State nextState) {
    if (nextN < 2) {
      logger.info("nextN = {}. skipping to done", nextN);
      execution.setVariable("result", execution.getVariable("result", Integer.class) + 1);
      return moveToState(nextState, "N - " + offset + " = " + nextN + ". Going to end.");
    }
    logger.info("Create child workflow N={}", nextN);
    execution.addChildWorkflows(createWorkflow(execution, nextN));
    return moveToState(nextState, "Creating childWorkflow to process f(" + nextN + ")");
  }

  public NextAction poll(StateExecution execution) {
    // get finished and failed child workflows
    QueryWorkflowInstances query = new QueryWorkflowInstances.Builder().addStatuses(WorkflowInstanceStatus.manual,
        WorkflowInstanceStatus.finished).build();
    List<WorkflowInstance> finishedChildren = execution.queryChildWorkflows(query);

    if (finishedChildren.size() < execution.getAllChildWorkflows().size()) {
      return retryAfter(now().plusSeconds(10), "Child workflows are not ready yet.");
    }
    int sum = 0;
    for (WorkflowInstance child : finishedChildren) {
      if (child.status != WorkflowInstanceStatus.finished) {
        return stopInState(State.error, "Some of the children failed");
      }
      String childResult = child.stateVariables.get("result");
      sum += parseInt(childResult != null ? childResult : "0");
    }
    execution.setVariable("result", execution.getVariable("result", Integer.class) + sum);
    execution.wakeUpParentWorkflow();
    return moveToState(State.done, "All is good");
  }

  public void done(StateExecution execution, @StateVar(value = "requestData") FiboData fiboData,
      @StateVar(value = "result") int result) {
    logger.info("We are done: fibonacci({}) == {}", fiboData.n, result);
  }

  public void error(StateExecution execution, @StateVar(value = "requestData") FiboData fiboData,
      @StateVar(value = "result") int result) {
    logger.error("Failed to compute F({})", fiboData.n);
  }

  private WorkflowInstance createWorkflow(StateExecution execution, int n) {
    return execution.workflowInstanceBuilder().setType(FibonacciWorkflow.WORKFLOW_TYPE)
        .putStateVariable("requestData", new FiboData(n)).build();
  }

  public static class FiboData {
    public int n;

    public FiboData() {
      // for Jackson
    }

    public FiboData(int n) {
      this();
      this.n = n;
    }
  }
}
