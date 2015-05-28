package com.nitorcreations.nflow.engine.internal.workflow;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;

@RunWith(MockitoJUnitRunner.class)
public class StateExecutionImplTest {
  StateExecutionImpl execution;
  StateExecution executionInterface;

  WorkflowInstance instance;

  @Mock
  ObjectStringMapper objectStringMapper;
  @Mock
  WorkflowInstanceDao workflowDao;
  @Mock
  WorkflowInstancePreProcessor workflowInstancePreProcessor;
  ArgumentCaptor<QueryWorkflowInstances> queryCaptor = ArgumentCaptor.forClass(QueryWorkflowInstances.class);

  @Before
  public void setup() {
    instance = new WorkflowInstance.Builder().setId(99).setExternalId("ext").setRetries(88).setState("myState")
            .setBusinessKey("business").build();
    execution = new StateExecutionImpl(instance, objectStringMapper, workflowDao, workflowInstancePreProcessor);
    executionInterface = execution;
  }

  @Test
  public void addChildWorkflows() {
    WorkflowInstance child1 = new WorkflowInstance.Builder().setBusinessKey("child1").build();
    WorkflowInstance child2 = new WorkflowInstance.Builder().setBusinessKey("child2").build();

    WorkflowInstance processedChild1 = mock(WorkflowInstance.class, "processed1");
    WorkflowInstance processedChild2 = mock(WorkflowInstance.class, "processed2");
    when(workflowInstancePreProcessor.process(child1)).thenReturn(processedChild1);
    when(workflowInstancePreProcessor.process(child2)).thenReturn(processedChild2);

    execution.addChildWorkflows(child1, child2);

    assertThat(execution.getNewChildWorkflows(), is(asList(processedChild1, processedChild2)));
  }

  @Test
  public void wakeUpParentWorkflowSetsWakeupFlag() {
    assertThat(execution.isWakeUpParentWorkflowSet(), is(false));
    execution.wakeUpParentWorkflow();
    assertThat(execution.isWakeUpParentWorkflowSet(), is(true));
    execution.wakeUpParentWorkflow();
    assertThat(execution.isWakeUpParentWorkflowSet(), is(true));
  }

  @Test
  public void queryChildWorkflowsIsRestrictedToChildsOfCurrentWorkflow() {
    QueryWorkflowInstances query = new QueryWorkflowInstances.Builder()
            .setParentActionId(42).addTypes("a","b")
            .setBusinessKey("123").build();

    List<WorkflowInstance> result = singletonList(mock(WorkflowInstance.class));
    when(workflowDao.queryWorkflowInstances(queryCaptor.capture())).thenReturn(result);

    assertThat(execution.queryChildWorkflows(query), is(result));
    QueryWorkflowInstances actualQuery = queryCaptor.getValue();

    assertThat(actualQuery.parentWorkflowId, is(99));
    assertThat(actualQuery.types, is(asList("a", "b")));
    assertThat(actualQuery.businessKey, is("123"));
  }

  @Test
  public void getAllChildWorkflowsQueriesAllChildWorkflows() {
    List<WorkflowInstance> result = singletonList(mock(WorkflowInstance.class));
    when(workflowDao.queryWorkflowInstances(queryCaptor.capture())).thenReturn(result);

    assertThat(execution.getAllChildWorkflows(), is(result));
    QueryWorkflowInstances actualQuery = queryCaptor.getValue();

    assertThat(actualQuery.parentWorkflowId, is(99));
    assertThat(actualQuery.types, is(Collections.<String>emptyList()));
    assertThat(actualQuery.businessKey, is(nullValue()));
  }

  @Test
  public void stateExecutionProvidesAccessToSomeWorkflowInstanceFields() {
    assertThat(instance.businessKey, is(executionInterface.getBusinessKey()));
    assertThat(instance.id, is(executionInterface.getWorkflowInstanceId()));
    assertThat(instance.externalId, is(executionInterface.getWorkflowInstanceExternalId()));
    assertThat(instance.retries, is(executionInterface.getRetries()));
  }

  @Test
  public void workflowInstanceBuilder() {
    WorkflowInstance.Builder builder = executionInterface.workflowInstanceBuilder();
    Object data = new Data(32, "foobar");
    String serializedData = "data in serialized form";
    when(objectStringMapper.convertFromObject("foo", data)).thenReturn(serializedData);
    assertThat(builder, is(notNullValue()));
    builder.putStateVariable("foo", data);
    WorkflowInstance i = builder.build();
    assertThat(i.nextActivation, is(notNullValue()));
    assertThat(i.stateVariables.get("foo"), is(serializedData));
    verify(objectStringMapper).convertFromObject("foo", data);
  }

  static class Data {
    public final int number;
    public final String text;
    public Data(int number, String text) {
      this.number  = number;
      this.text = text;
    }
  }
}
