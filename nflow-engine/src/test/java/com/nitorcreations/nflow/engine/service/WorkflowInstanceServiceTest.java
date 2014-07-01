package com.nitorcreations.nflow.engine.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.internal.executor.BaseNflowTest;
import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.WorkflowStateType;

public class WorkflowInstanceServiceTest extends BaseNflowTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Mock
  private WorkflowInstanceDao repositoryDao;
  @Mock
  private ClassPathResource nonSpringWorkflowListing;
  @Captor
  private ArgumentCaptor<WorkflowInstance> stored, stored2;
  @Captor
  private ArgumentCaptor<WorkflowInstanceAction> storedAction;

  private WorkflowInstanceService service;

  @Before
  public void setup() throws Exception {
    String dummyTestClassname = DummyTestWorkflow.class.getName();
    ByteArrayInputStream bis = new ByteArrayInputStream(dummyTestClassname.getBytes(UTF_8));
    when(nonSpringWorkflowListing.getInputStream()).thenReturn(bis);
    service = new WorkflowInstanceService(repositoryDao, nonSpringWorkflowListing);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(0)));
    service.initNonSpringWorkflowDefinitions();
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void getWorkflowInstance() {
    WorkflowInstance instance = Mockito.mock(WorkflowInstance.class);
    when(repositoryDao.getWorkflowInstance(42)).thenReturn(instance);
    assertEquals(instance, service.getWorkflowInstance(42));
  }

  @Test
  public void initDuplicateWorkflows() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Both com.nitorcreations.nflow.engine.service.WorkflowInstanceServiceTest$DummyTestWorkflow and com.nitorcreations.nflow.engine.service.WorkflowInstanceServiceTest$DummyTestWorkflow define same workflow type: dummy");
    String dummyTestClassname = DummyTestWorkflow.class.getName();
    ByteArrayInputStream bis = new ByteArrayInputStream((dummyTestClassname + "\n" + dummyTestClassname).getBytes(UTF_8));
    when(nonSpringWorkflowListing.getInputStream()).thenReturn(bis);
    service.initNonSpringWorkflowDefinitions();
  }

  @Test
  public void springWorkflowsWork() {
    List<WorkflowDefinition<? extends WorkflowState>> definitions = service.getWorkflowDefinitions();
    List<WorkflowDefinition<? extends WorkflowState>> list = new ArrayList<>();
    list.add(new SpringDummyTestWorkflow());
    service.setWorkflowDefinitions(list);
    assertThat(definitions.size(), is(equalTo(1)));
  }

  @Test
  public void demoWorkflowLoadedSuccessfully() {
    List<WorkflowDefinition<? extends WorkflowState>> definitions = service.getWorkflowDefinitions();
    assertThat(definitions.size(), is(equalTo(1)));
  }

  @Test
  public void whenBatchSizeIsZeroShouldNotPoll() {
    assertEquals(Collections.emptyList(), service.pollNextWorkflowInstanceIds(0));
    verifyZeroInteractions(repositoryDao);
  }

  @Test
  public void whenBatchSizeIsNonZeroShouldPoll() {
    List<Integer> ids = asList(2, 3, 5);
    when(repositoryDao.pollNextWorkflowInstanceIds(42)).thenReturn(ids);
    assertEquals(ids, service.pollNextWorkflowInstanceIds(42));
    verify(repositoryDao).pollNextWorkflowInstanceIds(42);
  }

  @Test
  public void insertWorkflowInstanceWorks() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setExternalId("123").build();
    when(repositoryDao.insertWorkflowInstance(stored.capture())).thenReturn(42);
    assertThat(service.insertWorkflowInstance(i), is(42));
    assertThat(stored.getValue().externalId, is("123"));
  }

  @Test
  public void insertWorkflowCreatesMissingExternalId() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().build();
    when(repositoryDao.insertWorkflowInstance(stored.capture())).thenReturn(42);
    service.insertWorkflowInstance(i);
    assertThat(stored.getValue().externalId, notNullValue());
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
    verify(repositoryDao).updateWorkflowInstance(i);
    verify(repositoryDao).insertWorkflowInstanceAction(i, a);
  }

  @Test
  public void listWorkflowInstances() {
    List<WorkflowInstance> result  = asList(constructWorkflowInstanceBuilder().build());
    QueryWorkflowInstances query = mock(QueryWorkflowInstances.class);
    when(repositoryDao.queryWorkflowInstances(query)).thenReturn(result);
    assertEquals(result, service.listWorkflowInstances(query));
  }

  @Test
  public void nonSpringWorkflowsAreOptional() throws Exception {
    service = new WorkflowInstanceService(repositoryDao, null);
    service.initNonSpringWorkflowDefinitions();
    assertEquals(0, service.getWorkflowDefinitions().size());
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

  public static class SpringDummyTestWorkflow extends WorkflowDefinition<SpringDummyTestWorkflow.SpringDummyTestState> {

    public static enum SpringDummyTestState implements com.nitorcreations.nflow.engine.workflow.WorkflowState {
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

    protected SpringDummyTestWorkflow() {
      super("springdummy", SpringDummyTestState.start, SpringDummyTestState.end);
    }

    public void start(StateExecution execution) {
      execution.setNextState(SpringDummyTestState.end);
    }

    public void end(StateExecution execution) {
      execution.setNextState(SpringDummyTestState.end);
    }

  }

}
