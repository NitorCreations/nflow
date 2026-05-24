package io.nflow.engine.internal.workflow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.nflow.engine.internal.workflow.WorkflowStateMethod.StateParameter;

public class WorkflowStateMethodTest {

  private final WorkflowStateMethod method = new WorkflowStateMethod(null,
      new StateParameter("foo", String.class, null, false, false),
      new StateParameter("bar", Integer.class, null, false, false));

  @Test
  public void hasParameterReturnsTrueForMatchingName() {
    assertTrue(method.hasParameter("foo"));
    assertTrue(method.hasParameter("bar"));
  }

  @Test
  public void hasParameterReturnsFalseForUnknownName() {
    assertFalse(method.hasParameter("baz"));
  }

  @Test
  public void hasParameterReturnsFalseWhenMethodHasNoParams() {
    WorkflowStateMethod methodWithoutParams = new WorkflowStateMethod(null);

    assertFalse(methodWithoutParams.hasParameter("foo"));
  }
}
