package io.nflow.rest.v1.jaxrs;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.engine.service.WorkflowExecutorService;
import io.nflow.engine.workflow.executor.WorkflowExecutor;
import io.nflow.rest.v1.converter.ListWorkflowExecutorConverter;

@ExtendWith(MockitoExtension.class)
public class WorkflowExecutorResourceTest {

  @Mock
  private WorkflowExecutorService workflowExecutors;

  @Mock
  private ListWorkflowExecutorConverter converter;

  @Mock
  private WorkflowExecutor executor;

  private WorkflowExecutorResource resource;

  @BeforeEach
  public void setup() {
    when(workflowExecutors.getWorkflowExecutors()).thenReturn(asList(executor));
    resource = new WorkflowExecutorResource(workflowExecutors, converter);
  }

  @Test
  public void listWorkflowExecutorsReturnsExistingExecutors() {
    try (Response listWorkflowExecutors = resource.listWorkflowExecutors()) {
      assertThat(listWorkflowExecutors.readEntity(List.class).size(), is(1));
    }
  }
}
