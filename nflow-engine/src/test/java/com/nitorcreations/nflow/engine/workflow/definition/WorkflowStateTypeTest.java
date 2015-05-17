package com.nitorcreations.nflow.engine.workflow.definition;

import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

public class WorkflowStateTypeTest {

  @Test
  public void getStatusReturnsCorrectStatusForStartState() {
    assertThat(WorkflowStateType.start.getStatus(null), is(WorkflowInstanceStatus.inProgress));
  }

  @Test
  public void getStatusReturnsCorrectStatusForNormalState() {
    assertThat(WorkflowStateType.normal.getStatus(null), is(WorkflowInstanceStatus.inProgress));
  }

  @Test
  public void getStatusReturnsCorrectStatusForEndState() {
    assertThat(WorkflowStateType.end.getStatus(null), is(WorkflowInstanceStatus.finished));
    assertThat(WorkflowStateType.end.getStatus(now()), is(WorkflowInstanceStatus.inProgress));
  }

  @Test
  public void getStatusReturnsCorrectStatusForManualState() {
    assertThat(WorkflowStateType.manual.getStatus(null), is(WorkflowInstanceStatus.manual));
    assertThat(WorkflowStateType.manual.getStatus(now()), is(WorkflowInstanceStatus.inProgress));
  }
}
