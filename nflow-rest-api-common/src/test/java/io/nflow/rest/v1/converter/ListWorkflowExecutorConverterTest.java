package io.nflow.rest.v1.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.nflow.engine.workflow.executor.WorkflowExecutor;
import io.nflow.rest.v1.msg.ListWorkflowExecutorResponse;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListWorkflowExecutorConverterTest {

  private final ListWorkflowExecutorConverter converter = new ListWorkflowExecutorConverter();

  @Test
  public void convertWorks() {
    WorkflowExecutor executor = new WorkflowExecutor(1, "host", 2, "executorGroup", now(),
            now().plusMinutes(1), now().plusMinutes(15), now().minusSeconds(7));

    ListWorkflowExecutorResponse resp = converter.convert(executor);

    assertThat(resp.id, is(executor.id));
    assertThat(resp.host, is(executor.host));
    assertThat(resp.pid, is(executor.pid));
    assertThat(resp.executorGroup, is(executor.executorGroup));
    assertThat(resp.started, is(executor.started));
    assertThat(resp.active, is(executor.active));
    assertThat(resp.expires, is(executor.expires));
    assertThat(resp.stopped, is(executor.stopped));
  }
}
