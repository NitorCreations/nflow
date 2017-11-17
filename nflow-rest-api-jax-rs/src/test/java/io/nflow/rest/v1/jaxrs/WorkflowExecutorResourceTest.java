package io.nflow.rest.v1.jaxrs;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.nflow.engine.service.WorkflowExecutorService;
import io.nflow.engine.workflow.executor.WorkflowExecutor;
import io.nflow.rest.v1.converter.ListWorkflowExecutorConverter;
import io.nflow.rest.v1.jaxrs.WorkflowExecutorResource;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowExecutorResourceTest {

  @Mock
  private WorkflowExecutorService workflowExecutors;

  @Mock
  private ListWorkflowExecutorConverter converter;

  @Mock
  private WorkflowExecutor executor;

  private WorkflowExecutorResource resource;

  @Before
  public void setup() {
    when(workflowExecutors.getWorkflowExecutors()).thenReturn(asList(executor));
    resource = new WorkflowExecutorResource(workflowExecutors, converter);
  }

  @Test
  public void listWorkflowExecutorsReturnsExistingExecutors() {
    assertThat(resource.listWorkflowExecutors().size(), is(1));
  }
}
