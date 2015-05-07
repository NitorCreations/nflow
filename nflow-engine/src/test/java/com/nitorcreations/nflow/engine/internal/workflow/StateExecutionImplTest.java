package com.nitorcreations.nflow.engine.internal.workflow;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StateExecutionImplTest {
  StateExecutionImpl execution;
  WorkflowInstance instance;

  @Mock
  ObjectStringMapper objectStringMapper;
  @Mock
  WorkflowInstanceDao workflowDao;

  ArgumentCaptor<QueryWorkflowInstances> queryCaptor = ArgumentCaptor.forClass(QueryWorkflowInstances.class);

  @Before
  public void setup() {
    instance = new WorkflowInstance.Builder().setId(99).build();
    execution = new StateExecutionImpl(instance, objectStringMapper, workflowDao);
  }

  @Test
  public void queryChildWorkflowsIsRestrictedToChildsOfCurrentWorkflow() {
    QueryWorkflowInstances query = new QueryWorkflowInstances.Builder()
            .setParentActionId(42).addTypes("a","b")
            .setBusinessKey("123").build();

    List<WorkflowInstance> result = Arrays.asList(mock(WorkflowInstance.class));
    when(workflowDao.queryWorkflowInstances(queryCaptor.capture())).thenReturn(result);

    assertThat(execution.queryChildWorkflows(query), is(result));
    QueryWorkflowInstances actualQuery = queryCaptor.getValue();

    assertThat(actualQuery.parentWorkflowId, is(99));
    assertThat(actualQuery.types, is(Arrays.asList("a", "b")));
    assertThat(actualQuery.businessKey, is("123"));
  }
}
