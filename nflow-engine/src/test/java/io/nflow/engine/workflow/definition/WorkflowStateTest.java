package io.nflow.engine.workflow.definition;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

public class WorkflowStateTest {

  private final TestWorkflowState state = new TestWorkflowState();

  @Test
  public void getDescriptionReturnsNameByDefault() {
    assertThat(state.getDescription(), is(state.name()));
  }

  @Test
  public void isRetryAllowedReturnsTrueByDefault() {
    assertTrue(state.isRetryAllowed());
  }

  static class TestWorkflowState implements WorkflowState {

    @Override
    public String name() {
      return "name";
    }

    @Override
    public WorkflowStateType getType() {
      return WorkflowStateType.normal;
    }
  }
}
