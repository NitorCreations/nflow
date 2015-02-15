package com.nitorcreations.nflow.engine.internal.executor;

import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;

import java.util.LinkedHashMap;
import java.util.UUID;

import org.joda.time.DateTime;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

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
      .setExternalId(UUID.randomUUID().toString())
      .setRetries(0)
      .setExecutorGroup("flowInstance1")
      .setStateVariables(new LinkedHashMap<String,String>() {{put("requestData", "{ \"parameter\": \"abc\" }"); }});
  }

  protected WorkflowInstance.Builder executingInstanceBuilder() {
    return constructWorkflowInstanceBuilder().setId(1).setStatus(executing);
  }
}
