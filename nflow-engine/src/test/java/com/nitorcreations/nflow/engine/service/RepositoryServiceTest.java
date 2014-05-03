package com.nitorcreations.nflow.engine.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import com.nitorcreations.nflow.engine.BaseNflowTest;
import com.nitorcreations.nflow.engine.dao.RepositoryDao;
import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.WorkflowStateType;

public class RepositoryServiceTest extends BaseNflowTest {

  @Mock
  private RepositoryDao repositoryDao;

  @Mock
  private ApplicationContext appCtx;

  @Mock
  private ClassPathResource workflowDefinitionListing;

  private RepositoryService service;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
    when(appCtx.getBean(Mockito.any(Class.class))).thenThrow(NoSuchBeanDefinitionException.class);
    String dummyTestClassname = DummyTestWorkflow.class.getName();
    ByteArrayInputStream bis = new ByteArrayInputStream(dummyTestClassname.getBytes(UTF_8));
    when(workflowDefinitionListing.getInputStream()).thenReturn(bis);
    service = new RepositoryService(repositoryDao, appCtx, workflowDefinitionListing);
  }

  @Test
  public void demoWorkflowLoadedSuccessfully() throws Exception {
    service.initWorkflowDefinitions();
    List<WorkflowDefinition<? extends WorkflowState>> definitions = service.getWorkflowDefinitions();
    assertThat(definitions.size(), is(1));
  }

  public static class DummyTestWorkflow extends WorkflowDefinition<DummyTestWorkflow.DummyTestState> {

    public static enum DummyTestState implements com.nitorcreations.nflow.engine.workflow.WorkflowState {
      start, end;

      @Override
      public WorkflowStateType getType() {
        return null;
      }

      @Override
      public String getName() {
        return null;
      }

      @Override
      public String getDescription() {
        return null;
      }

    }

    protected DummyTestWorkflow() {
      super("dummy", DummyTestState.start, DummyTestState.end);
    }

    public void start(StateExecution execution) {
      execution.setNextState(DummyTestState.end);
    }

    public void end(StateExecution execution) {
      execution.setNextState(DummyTestState.end);
    }

  }

}
