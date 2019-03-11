package io.nflow.rest.v1.jaxrs;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import io.nflow.engine.service.WorkflowExecutorService;
import io.nflow.engine.workflow.executor.WorkflowExecutor;
import io.nflow.rest.v1.converter.ListWorkflowExecutorConverter;
import org.mockito.junit.jupiter.MockitoExtension;

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
    assertThat(resource.listWorkflowExecutors().size(), is(1));
  }
}
