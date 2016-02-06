package com.nitorcreations.nflow.engine.workflow.instance;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class WorkflowInstanceTest {

  @Test
  public void newWorkflowInstanceNextActivationIsSetByDefault() {
    WorkflowInstance instance = new WorkflowInstance.Builder().build();
    assertThat(instance.nextActivation, is(not(nullValue())));
  }
}
