package com.nitorcreations.nflow.engine.service;

import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.internal.executor.BaseNflowTest;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

public class WorkflowInstanceServiceTest extends BaseNflowTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Mock
  private WorkflowDefinitionService workflowDefinitions;
  @Mock
  private WorkflowInstanceDao workflowInstanceDao;
  @Mock
  private WorkflowInstancePreProcessor workflowInstancePreProcessor;
  @Captor
  private ArgumentCaptor<WorkflowInstance> stored, stored2;
  @Captor
  private ArgumentCaptor<WorkflowInstanceAction> storedAction;
  @Captor
  private ArgumentCaptor<QueryWorkflowInstances> queryCapture;

  private WorkflowInstanceService service;

  @Before
  public void setup() {
    WorkflowDefinition<?> dummyWorkflow = new DummyTestWorkflow();
    doReturn(dummyWorkflow).when(workflowDefinitions).getWorkflowDefinition("dummy");
    service = new WorkflowInstanceService(workflowDefinitions, workflowInstanceDao, workflowInstancePreProcessor);
    setCurrentMillisFixed(currentTimeMillis());
  }

  @After
  public void reset() {
    setCurrentMillisSystem();
  }

  @Test
  public void getWorkflowInstance() {
    WorkflowInstance instance = Mockito.mock(WorkflowInstance.class);
    when(workflowInstanceDao.getWorkflowInstance(42)).thenReturn(instance);
    assertEquals(instance, service.getWorkflowInstance(42));
  }

  @Test
  public void insertWorkflowInstanceWorks() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setStatus(created).setExternalId("123").setState(null).build();
    when(workflowInstancePreProcessor.process(i)).thenReturn(i);
    when(workflowInstanceDao.insertWorkflowInstance(stored.capture())).thenReturn(42);
    assertThat(service.insertWorkflowInstance(i), is(42));
    assertThat(stored.getValue().externalId, is("123"));
    assertThat(stored.getValue().status, is(created));
  }

  @Test
  public void insertWorkflowInstanceWhenPreprocessorThrowsCausesException() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage("preprocessor reject");
    WorkflowInstance i = constructWorkflowInstanceBuilder().setType("nonexistent").build();
    when(workflowInstancePreProcessor.process(i)).thenThrow(new RuntimeException("preprocessor reject"));
    service.insertWorkflowInstance(i);
  }

  @Test
  public void updateWorkflowInstanceWorks() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setId(42).build();
    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().setType(externalChange).setWorkflowInstanceId(i.id).build();
    when(workflowInstanceDao.getWorkflowInstanceState(i.id)).thenReturn("currentState");
    when(workflowInstanceDao.updateNotRunningWorkflowInstance(any(WorkflowInstance.class))).thenReturn(true);
    when(workflowInstanceDao.getWorkflowInstance(42)).thenReturn(i);
    assertThat(service.updateWorkflowInstance(i, a), is(true));
    verify(workflowInstanceDao).updateNotRunningWorkflowInstance(stored.capture());
    assertThat(stored.getValue().status, is(inProgress));
    verify(workflowInstanceDao).insertWorkflowInstanceAction(stored2.capture(), storedAction.capture());
    assertThat(storedAction.getValue().state, is("currentState"));
  }

  @Test
  public void updateWorkflowInstanceThrowsExceptionWhenActionIsNull() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(is("Workflow instance action can not be null"));
    WorkflowInstance i = constructWorkflowInstanceBuilder().setId(42).build();
    WorkflowInstanceAction a = null;
    service.updateWorkflowInstance(i, a);
  }

  @Test
  public void updateWorkflowInstanceWorksWhenStateIsNull() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setId(42).setState(null).build();
    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().setType(externalChange).setWorkflowInstanceId(i.id).build();
    when(workflowInstanceDao.getWorkflowInstanceState(i.id)).thenReturn("currentState");
    when(workflowInstanceDao.updateNotRunningWorkflowInstance(any(WorkflowInstance.class))).thenReturn(true);
    assertThat(service.updateWorkflowInstance(i, a), is(true));
    verify(workflowInstanceDao).updateNotRunningWorkflowInstance(stored.capture());
    assertThat(stored.getValue().status, is(nullValue()));
    verify(workflowInstanceDao).insertWorkflowInstanceAction(stored2.capture(), storedAction.capture());
    assertThat(storedAction.getValue().state, is("currentState"));
  }

  @Test
  public void updateRunningWorkflowInstanceFails() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setId(42).build();
    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().setType(externalChange).build();
    when(workflowInstanceDao.updateNotRunningWorkflowInstance(i)).thenReturn(false);
    when(workflowInstanceDao.getWorkflowInstance(42)).thenReturn(i);
    assertThat(service.updateWorkflowInstance(i, a), is(false));
    verify(workflowInstanceDao, never()).insertWorkflowInstanceAction(any(WorkflowInstance.class),
        any(WorkflowInstanceAction.class));
  }

  @Test
  public void wakeUpWorkflowInstance() {
    String[] states = new String[] {"abc", "xyz"};
    service.wakeupWorkflowInstance(99, states);
    verify(workflowInstanceDao).wakeupWorkflowInstanceIfNotExecuting(99L, states);
  }

  @Test
  public void listWorkflowInstances() {
    List<WorkflowInstance> result  = asList(constructWorkflowInstanceBuilder().build());
    QueryWorkflowInstances query = mock(QueryWorkflowInstances.class);
    when(workflowInstanceDao.queryWorkflowInstances(query)).thenReturn(result);
    assertEquals(result, service.listWorkflowInstances(query));
  }

}
