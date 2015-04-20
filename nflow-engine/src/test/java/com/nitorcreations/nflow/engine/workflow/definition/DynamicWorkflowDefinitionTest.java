package com.nitorcreations.nflow.engine.workflow.definition;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.nitorcreations.nflow.engine.internal.workflow.WorkflowStateMethod;

public class DynamicWorkflowDefinitionTest {
  @Test
  public void create() {
    AbstractWorkflowDefinition<?> def = new WorkflowDefinitionBuilder("dynamic")
        .setInitialState(DynamicWorkflowDefinitionTest.class, "method", "start").setErrorState(WorkflowStateType.manual, "error")
        .build();
    WorkflowStateMethod m = def.getMethod("start");
    assertThat(m.method.getName(), is("method"));
    assertThat(m.method.getDeclaringClass(), is((Object) DynamicWorkflowDefinitionTest.class));
  }

  public NextAction method(StateExecution execution) {
    return null;
  }
}
