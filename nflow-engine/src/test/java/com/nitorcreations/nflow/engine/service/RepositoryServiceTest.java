package com.nitorcreations.nflow.engine.service;

import static com.nitorcreations.Matchers.hasField;
import static com.nitorcreations.Matchers.reflectEquals;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.io.ClassPathResource;

import com.nitorcreations.nflow.engine.BaseNflowTest;
import com.nitorcreations.nflow.engine.dao.RepositoryDao;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.domain.WorkflowInstanceAction;
import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.WorkflowStateType;

public class RepositoryServiceTest extends BaseNflowTest {

  @Mock
  private RepositoryDao repositoryDao;

  @Mock
  private ClassPathResource nonSpringWorkflowListing;

  private RepositoryService service;

  @Before
  public void setup() throws Exception {
    String dummyTestClassname = DummyTestWorkflow.class.getName();
    ByteArrayInputStream bis = new ByteArrayInputStream(dummyTestClassname.getBytes(UTF_8));
    when(nonSpringWorkflowListing.getInputStream()).thenReturn(bis);
    service = new RepositoryService(repositoryDao, nonSpringWorkflowListing);
    service.initNonSpringWorkflowDefinitions();
  }

  @Test(expected = RuntimeException.class)
  public void initDuplicateWorkflows() throws Exception {
    String dummyTestClassname = DummyTestWorkflow.class.getName();
    ByteArrayInputStream bis = new ByteArrayInputStream((dummyTestClassname + "\n" + dummyTestClassname).getBytes(UTF_8));
    when(nonSpringWorkflowListing.getInputStream()).thenReturn(bis);
    service.initNonSpringWorkflowDefinitions();
  }

  @Test
  public void demoWorkflowLoadedSuccessfully() {
    List<WorkflowDefinition<? extends WorkflowState>> definitions = service.getWorkflowDefinitions();
    assertThat(definitions.size(), is(equalTo(1)));
  }

  @Test
  public void insertWorkflowInstanceWorks() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().build();
    service.insertWorkflowInstance(i);
    verify(repositoryDao).insertWorkflowInstance((WorkflowInstance)argThat(allOf(
        hasField("externalId", notNullValue(WorkflowInstance.class)),
        hasField("created", notNullValue(WorkflowInstance.class)),
        hasField("modified", notNullValue(WorkflowInstance.class)))));
  }

  @Test(expected = RuntimeException.class)
  public void insertWorkflowInstanceUnsupportedType() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setType("nonexistent").build();
    service.insertWorkflowInstance(i);
  }

  @Test
  public void updateWorkflowInstanceWorks() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().build();
    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().build();
    service.updateWorkflowInstance(i, a);
    verify(repositoryDao).updateWorkflowInstance(argThat(reflectEquals(i, "modified")));
    verify(repositoryDao).insertWorkflowInstanceAction(argThat(reflectEquals(i, "modified")), argThat(equalTo(a)));
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
