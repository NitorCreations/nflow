package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.tests.demo.workflow.TestState.BEGIN;
import static io.nflow.tests.demo.workflow.TestState.DONE;
import static io.nflow.tests.demo.workflow.TestState.ERROR;
import static io.nflow.tests.demo.workflow.TestState.POLL;
import static java.lang.Integer.parseInt;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.slf4j.Logger;

import io.nflow.engine.workflow.curated.SimpleState;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

/**
 * Fibonacci series generator using recursive process. Each step is handled by a new child workflow.
 */
public class FibonacciWorkflow extends AbstractWorkflowDefinition {
  public static final String FIBONACCI_TYPE = "fibonacci";
  public static final String VAR_REQUEST_DATA = "requestData";
  private static final Logger logger = getLogger(FibonacciWorkflow.class);

  private static final WorkflowState N_MINUS_1 = new SimpleState("nMinus1");
  private static final WorkflowState N_MINUS_2 = new SimpleState("nMinus2");

  public FibonacciWorkflow() {
    super(FIBONACCI_TYPE, BEGIN, ERROR);
    setDescription("Fibonacci series generator using recursive process. Each step is handled by a new child workflow.");
    permit(BEGIN, N_MINUS_2);
    permit(N_MINUS_2, N_MINUS_1);
    permit(N_MINUS_2, POLL);
    permit(N_MINUS_1, POLL);
    permit(N_MINUS_1, DONE);
    permit(N_MINUS_2, DONE);
    permit(POLL, DONE);
  }

  public NextAction begin(StateExecution execution, @StateVar(value = VAR_REQUEST_DATA, readOnly = true) FiboData fiboData) {
    int n = fiboData.n;
    logger.info("Fibonacci step N = {}", n);
    execution.setVariable("result", 0);
    return moveToState(N_MINUS_2, "Starting N = " + n);
  }

  public NextAction nMinus2(StateExecution execution, @StateVar(value = VAR_REQUEST_DATA, readOnly = true) FiboData fiboData) {
    int n = fiboData.n;
    return nextStep(execution, n - 2, 2, N_MINUS_1);
  }

  public NextAction nMinus1(StateExecution execution, @StateVar(value = VAR_REQUEST_DATA, readOnly = true) FiboData fiboData) {
    int n = fiboData.n;
    return nextStep(execution, n - 1, 1, POLL);
  }

  private NextAction nextStep(StateExecution execution, int nextN, int offset, WorkflowState nextState) {
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
        WorkflowInstanceStatus.finished).setIncludeCurrentStateVariables(true).build();
    List<WorkflowInstance> finishedChildren = execution.queryChildWorkflows(query);

    if (finishedChildren.size() < execution.getAllChildWorkflows().size()) {
      return retryAfter(now().plusSeconds(10), "Child workflows are not ready yet.");
    }
    int sum = 0;
    for (WorkflowInstance child : finishedChildren) {
      if (child.status != WorkflowInstanceStatus.finished) {
        return stopInState(ERROR, "Some of the children failed");
      }
      String childResult = child.stateVariables.get("result");
      sum += parseInt(childResult != null ? childResult : "0");
    }
    execution.setVariable("result", execution.getVariable("result", Integer.class) + sum);
    return moveToState(DONE, "All is good");
  }

  public void done(@SuppressWarnings("unused") StateExecution execution, @StateVar(value = VAR_REQUEST_DATA) FiboData fiboData,
      @StateVar(value = "result") int result) {
    logger.info("We are done: fibonacci({}) == {}", fiboData.n, result);
  }

  public void error(@SuppressWarnings("unused") StateExecution execution, @StateVar(value = VAR_REQUEST_DATA) FiboData fiboData,
      @SuppressWarnings("unused") @StateVar(value = "result") int result) {
    logger.error("Failed to compute F({})", fiboData.n);
  }

  private WorkflowInstance createWorkflow(StateExecution execution, int n) {
    return execution.workflowInstanceBuilder().setType(FibonacciWorkflow.FIBONACCI_TYPE)
        .putStateVariable(VAR_REQUEST_DATA, new FiboData(n)).build();
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
