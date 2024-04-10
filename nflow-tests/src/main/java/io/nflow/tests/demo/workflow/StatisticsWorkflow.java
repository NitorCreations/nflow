package io.nflow.tests.demo.workflow;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import org.springframework.stereotype.Component;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.tests.demo.workflow.TestState.*;

@Component
public class StatisticsWorkflow extends WorkflowDefinition {

    public static final String STATISTICS_WORKFLOW_TYPE = "statistics";

    @SuppressWarnings("this-escape")
    public StatisticsWorkflow() {
      super(STATISTICS_WORKFLOW_TYPE, BEGIN, ERROR);
      setDescription("Statistics workflow: start -> process -> end");
      permit(BEGIN, PROCESS);
      permit(PROCESS, DONE);
    }

    public NextAction begin(@SuppressWarnings("unused") StateExecution execution) {
      return moveToState(PROCESS, "Go to process state");
    }

    public NextAction process(@SuppressWarnings("unused") StateExecution execution) {
      return stopInState(DONE, "Go to done state");
    }
  }
