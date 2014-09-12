package com.nitorcreations.nflow.engine.internal.executor;

import static java.lang.Boolean.FALSE;

import java.util.LinkedHashMap;
import java.util.UUID;

import org.joda.time.DateTime;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("serial")
public abstract class BaseNflowTest {

  protected WorkflowInstance.Builder constructWorkflowInstanceBuilder() {
    return new WorkflowInstance.Builder()
      .setType("dummy")
      .setState("CreateLoan")
      .setStateText(null)
      .setNextActivation(new DateTime())
      .setExternalId(UUID.randomUUID().toString())
      .setProcessing(FALSE)
      .setRetries(0)
      .setOwner("flowInstance1")
      .setStateVariables(new LinkedHashMap<String,String>() {{put("req", "{ \"parameter\": \"abc\" }"); }});
  }

}
