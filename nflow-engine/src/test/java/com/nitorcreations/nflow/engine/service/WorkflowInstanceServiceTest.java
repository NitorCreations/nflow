package com.nitorcreations.nflow.engine.service;

import static com.nitorcreations.Matchers.containsElementsInAnyOrder;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.internal.executor.BaseNflowTest;
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
    Mockito.doReturn(dummyWorkflow).when(workflowDefinitions).getWorkflowDefinition("dummy");
    service = new WorkflowInstanceService(workflowDefinitions, workflowInstanceDao);
  }

  @Test
  public void getWorkflowInstance() {
    WorkflowInstance instance = Mockito.mock(WorkflowInstance.class);
    when(workflowInstanceDao.getWorkflowInstance(42)).thenReturn(instance);
    assertEquals(instance, service.getWorkflowInstance(42));
  }

  @Test
  public void whenBatchSizeIsZeroShouldNotPoll() {
    assertEquals(Collections.emptyList(), service.pollNextWorkflowInstanceIds(0));
    verifyZeroInteractions(workflowInstanceDao);
  }

  @Test
  public void whenBatchSizeIsNonZeroShouldPoll() {
    List<Integer> ids = asList(2, 3, 5);
    when(workflowInstanceDao.pollNextWorkflowInstanceIds(42)).thenReturn(ids);
    assertEquals(ids, service.pollNextWorkflowInstanceIds(42));
    verify(workflowInstanceDao).pollNextWorkflowInstanceIds(42);
  }

  @Test
  public void insertWorkflowInstanceWorks() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setExternalId("123").build();
    when(workflowInstanceDao.insertWorkflowInstance(stored.capture())).thenReturn(42);
    assertThat(service.insertWorkflowInstance(i), is(42));
    assertThat(stored.getValue().externalId, is("123"));
  }

  @Test
  public void insertDuplicateWorkflowInstanceFetchesExistingId() {
    List<WorkflowInstance> list  = singletonList(constructWorkflowInstanceBuilder().setId(43).build());
    WorkflowInstance i = constructWorkflowInstanceBuilder().setExternalId("123").build();
    when(workflowInstanceDao.insertWorkflowInstance(stored.capture())).thenReturn(-1);
    when(workflowInstanceDao.queryWorkflowInstances(queryCapture.capture())).thenReturn(list);
    assertThat(service.insertWorkflowInstance(i), is(43));
    assertThat(queryCapture.getValue().types, containsElementsInAnyOrder(singletonList("dummy")));
    assertThat(queryCapture.getValue().externalId, is("123"));
  }

  @Test
  public void insertWorkflowCreatesMissingExternalId() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().build();
    when(workflowInstanceDao.insertWorkflowInstance(stored.capture())).thenReturn(42);
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
    verify(workflowInstanceDao).updateWorkflowInstance(i);
    verify(workflowInstanceDao).insertWorkflowInstanceAction(i, a);
  }

  @Test
  public void listWorkflowInstances() {
    List<WorkflowInstance> result  = asList(constructWorkflowInstanceBuilder().build());
    QueryWorkflowInstances query = mock(QueryWorkflowInstances.class);
    when(workflowInstanceDao.queryWorkflowInstances(query)).thenReturn(result);
    assertEquals(result, service.listWorkflowInstances(query));
  }

}
