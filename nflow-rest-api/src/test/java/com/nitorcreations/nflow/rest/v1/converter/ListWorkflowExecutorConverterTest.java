package com.nitorcreations.nflow.rest.v1.converter;

import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.workflow.executor.WorkflowExecutor;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowExecutorResponse;

@RunWith(MockitoJUnitRunner.class)
public class ListWorkflowExecutorConverterTest {

  private final ListWorkflowExecutorConverter converter = new ListWorkflowExecutorConverter();

  @Test
  public void convertWorks() {
    WorkflowExecutor executor = new WorkflowExecutor(1, "host", 2, "executorGroup", now(), now().plusMinutes(1), now()
        .plusMinutes(15));

    ListWorkflowExecutorResponse resp = converter.convert(executor);

    assertThat(resp.id, is(executor.id));
    assertThat(resp.host, is(executor.host));
    assertThat(resp.pid, is(executor.pid));
    assertThat(resp.executorGroup, is(executor.executorGroup));
    assertThat(resp.started, is(executor.started));
    assertThat(resp.active, is(executor.active));
    assertThat(resp.expires, is(executor.expires));
  }
}
