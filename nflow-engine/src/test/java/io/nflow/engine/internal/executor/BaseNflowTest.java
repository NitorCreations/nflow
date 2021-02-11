package io.nflow.engine.internal.executor;

import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static java.util.UUID.randomUUID;

import java.util.LinkedHashMap;
import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.engine.service.DummyTestWorkflow;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("serial")
public abstract class BaseNflowTest {

  protected WorkflowInstance.Builder constructWorkflowInstanceBuilder() {
    return new WorkflowInstance.Builder() //
        .setStatus(WorkflowInstanceStatus.inProgress) //
        .setType(DummyTestWorkflow.DUMMY_TYPE) //
        .setState(DummyTestWorkflow.CREATE_LOAN.name()) //
        .setStateText(null) //
        .setExternalId(randomUUID().toString()) //
        .setBusinessKey(randomUUID().toString()) //
        .setRetries(0) //
        .setExecutorGroup("flowInstance1") //
        .setStateVariables(new LinkedHashMap<String, String>() {
          {
            put("requestData", "{ \"parameter\": \"abc\" }");
          }
        }) //
        .setSignal(Optional.of(42)) //
        .setPriority((short) 0);
  }

  protected WorkflowInstanceAction.Builder constructActionBuilder(long workflowInstanceID) {
    return new WorkflowInstanceAction.Builder() //
        .setExecutionStart(DateTime.now()) //
        .setExecutorId(42) //
        .setExecutionEnd(DateTime.now().plusMillis(100)) //
        .setRetryNo(1) //
        .setType(stateExecution) //
        .setState("test") //
        .setStateText("state text") //
        .setWorkflowInstanceId(workflowInstanceID);
  }

  protected WorkflowInstance.Builder executingInstanceBuilder() {
    return constructWorkflowInstanceBuilder().setId(1).setStatus(executing);
  }
}
