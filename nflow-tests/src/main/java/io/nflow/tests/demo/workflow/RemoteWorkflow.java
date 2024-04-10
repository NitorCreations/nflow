package io.nflow.tests.demo.workflow;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowDefinition;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.stopInState;
import static io.nflow.tests.demo.workflow.TestState.*;

public class RemoteWorkflow extends WorkflowDefinition {

    public static final String REMOTE_WORKFLOW_TYPE = "remote";

    @SuppressWarnings("this-escape")
    public RemoteWorkflow() {
      super(REMOTE_WORKFLOW_TYPE, BEGIN, ERROR);
      setDescription("Remote workflow: start -> process -> end");
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
