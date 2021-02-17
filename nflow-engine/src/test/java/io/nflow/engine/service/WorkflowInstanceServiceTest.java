package io.nflow.engine.service;

import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.executor.BaseNflowTest;
import io.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;

public class WorkflowInstanceServiceTest extends BaseNflowTest {

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

  @BeforeEach
  public void setup() {
    AbstractWorkflowDefinition dummyWorkflow = new DummyTestWorkflow();
    lenient().doReturn(dummyWorkflow).when(workflowDefinitions).getWorkflowDefinition("dummy");
    service = new WorkflowInstanceService(workflowInstanceDao, workflowDefinitions, workflowInstancePreProcessor);
    setCurrentMillisFixed(currentTimeMillis());
  }

  @AfterEach
  public void reset() {
    setCurrentMillisSystem();
  }

  @Test
  public void getWorkflowInstance() {
    WorkflowInstance instance = Mockito.mock(WorkflowInstance.class);
    @SuppressWarnings("unchecked")
    Set<WorkflowInstanceInclude> includes = Mockito.mock(Set.class);
    when(workflowInstanceDao.getWorkflowInstance(42, includes, 10L)).thenReturn(instance);
    assertEquals(instance, service.getWorkflowInstance(42, includes, 10L));
  }

  @Test
  public void insertWorkflowInstanceWorks() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setStatus(created).setExternalId("123").setState(null).build();
    when(workflowInstancePreProcessor.process(i)).thenReturn(i);
    when(workflowInstanceDao.insertWorkflowInstance(stored.capture())).thenReturn(42L);
    assertThat(service.insertWorkflowInstance(i), is(42L));
    assertThat(stored.getValue().externalId, is("123"));
    assertThat(stored.getValue().status, is(created));
  }

  @Test
  public void insertWorkflowInstanceWhenPreprocessorThrowsCausesException() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setType("nonexistent").build();
    when(workflowInstancePreProcessor.process(i)).thenThrow(new RuntimeException("preprocessor reject"));
    RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.insertWorkflowInstance(i));
    assertThat(thrown.getMessage(), containsString("preprocessor reject"));
  }

  @Test
  public void updateWorkflowInstanceWorks() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setId(42).build();
    WorkflowInstanceAction a = new WorkflowInstanceAction.Builder().setType(externalChange).setWorkflowInstanceId(i.id).build();
    when(workflowInstanceDao.getWorkflowInstanceState(i.id)).thenReturn("currentState");
    when(workflowInstanceDao.updateNotRunningWorkflowInstance(any(WorkflowInstance.class))).thenReturn(true);
    when(workflowInstanceDao.getWorkflowInstanceType(42)).thenReturn(i.type);
    assertThat(service.updateWorkflowInstance(i, a), is(true));
    verify(workflowInstanceDao).updateNotRunningWorkflowInstance(stored.capture());
    assertThat(stored.getValue().status, is(inProgress));
    verify(workflowInstanceDao).insertWorkflowInstanceAction(stored2.capture(), storedAction.capture());
    assertThat(storedAction.getValue().state, is("currentState"));
  }

  @Test
  public void updateWorkflowInstanceThrowsExceptionWhenActionIsNull() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setId(42).build();
    WorkflowInstanceAction a = null;
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> service.updateWorkflowInstance(i, a));
    assertThat(thrown.getMessage(), is("Workflow instance action can not be null"));
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
    when(workflowInstanceDao.getWorkflowInstanceType(42)).thenReturn(i.type);
    assertThat(service.updateWorkflowInstance(i, a), is(false));
    verify(workflowInstanceDao, never()).insertWorkflowInstanceAction(any(WorkflowInstance.class),
        any(WorkflowInstanceAction.class));
  }

  @Test
  public void wakeUpWorkflowInstance() {
    List<String> states = asList("abc", "xyz");
    service.wakeupWorkflowInstance(99, states);
    verify(workflowInstanceDao).wakeupWorkflowInstanceIfNotExecuting(99, states);
  }

  @Test
  public void listWorkflowInstances() {
    List<WorkflowInstance> result  = asList(constructWorkflowInstanceBuilder().build());
    QueryWorkflowInstances query = mock(QueryWorkflowInstances.class);
    when(workflowInstanceDao.queryWorkflowInstances(query)).thenReturn(result);
    assertEquals(result, service.listWorkflowInstances(query));
  }

  @Test
  public void getSignalWorks() {
    when(workflowInstanceDao.getSignal(99)).thenReturn(Optional.of(42));

    assertThat(service.getSignal(99), is(Optional.of(42)));
  }

  @Test
  public void setSignalWorks() {
    when(workflowInstanceDao.getWorkflowInstanceType(99)).thenReturn("type");
    AbstractWorkflowDefinition definition = mock(AbstractWorkflowDefinition.class);
    doReturn(definition).when(workflowDefinitions).getWorkflowDefinition("type");
    when(definition.getSupportedSignals()).thenReturn(Collections.singletonMap(42, "supported"));

    service.setSignal(99, Optional.of(42), "testing", WorkflowActionType.stateExecution);

    verify(workflowInstanceDao).setSignal(99, Optional.of(42), "testing", WorkflowActionType.stateExecution);
    verify(workflowInstanceDao).getWorkflowInstanceType(99);
    verify(workflowDefinitions).getWorkflowDefinition("type");
  }

  @Test
  public void setSignalWorksWithUnsupportedSignal() {
    when(workflowInstanceDao.getWorkflowInstanceType(99)).thenReturn("type");
    AbstractWorkflowDefinition definition = mock(AbstractWorkflowDefinition.class);
    doReturn(definition).when(workflowDefinitions).getWorkflowDefinition("type");
    when(definition.getSupportedSignals()).thenReturn(Collections.singletonMap(42, "supported"));

    service.setSignal(99, Optional.of(1), "testing", WorkflowActionType.stateExecution);

    verify(workflowInstanceDao).setSignal(99, Optional.of(1), "testing", WorkflowActionType.stateExecution);
    verify(workflowInstanceDao).getWorkflowInstanceType(99);
    verify(workflowDefinitions).getWorkflowDefinition("type");
  }

  @Test
  public void resetSignalDoesNotQueryWorkflowDefinition() {
    service.setSignal(99, Optional.empty(), "testing", WorkflowActionType.stateExecution);

    verify(workflowInstanceDao).setSignal(99, Optional.empty(), "testing", WorkflowActionType.stateExecution);
    verify(workflowInstanceDao, never()).getWorkflowInstanceType(99);
    verify(workflowDefinitions, never()).getWorkflowDefinition(anyString());
  }

}
