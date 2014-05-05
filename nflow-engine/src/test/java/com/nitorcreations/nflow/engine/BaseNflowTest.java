package com.nitorcreations.nflow.engine;

import static java.lang.Boolean.FALSE;

import java.util.LinkedHashMap;

import org.joda.time.DateTime;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.domain.WorkflowInstance;

@RunWith(MockitoJUnitRunner.class)
public abstract class BaseNflowTest {

  protected WorkflowInstance.Builder constructWorkflowInstanceBuilder() {
    return new WorkflowInstance.Builder()
      .setType("dummy")
      .setRequestData("{ \"parameter\": \"abc\" }")
      .setState("CreateLoan")
      .setStateText(null)
      .setNextActivation(new DateTime())
      .setProcessing(FALSE)
      .setRetries(0)
      .setOwner("flowInstance1")
      .setStateVariables(new LinkedHashMap<String,String>());
  }

}
