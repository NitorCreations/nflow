package com.nitorcreations.nflow.engine.internal.executor;

import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static java.util.UUID.randomUUID;

import java.util.LinkedHashMap;

import org.joda.time.DateTime;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("serial")
public abstract class BaseNflowTest {

  protected WorkflowInstance.Builder constructWorkflowInstanceBuilder() {
    return new WorkflowInstance.Builder()
      .setStatus(WorkflowInstanceStatus.inProgress)
      .setType("dummy")
      .setState("CreateLoan")
      .setStateText(null)
      .setNextActivation(new DateTime())
      .setExternalId(randomUUID().toString())
      .setBusinessKey(randomUUID().toString())
      .setRetries(0)
      .setExecutorGroup("flowInstance1")
      .setStateVariables(new LinkedHashMap<String,String>() {{put("requestData", "{ \"parameter\": \"abc\" }"); }});
  }

  protected WorkflowInstanceAction.Builder constructActionBuilder(int workflowInstanceID) {
    return new WorkflowInstanceAction.Builder().setExecutionStart(DateTime.now()).setExecutorId(42)
            .setExecutionEnd(DateTime.now().plusMillis(100)).setRetryNo(1).setType(stateExecution).setState("test")
            .setStateText("state text")
            .setWorkflowInstanceId(workflowInstanceID);
  }
  protected WorkflowInstance.Builder executingInstanceBuilder() {
    return constructWorkflowInstanceBuilder().setId(1).setStatus(executing);
  }
}
