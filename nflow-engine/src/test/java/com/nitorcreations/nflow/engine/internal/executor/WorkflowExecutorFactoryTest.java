package com.nitorcreations.nflow.engine.internal.executor;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;

public class WorkflowExecutorFactoryTest extends BaseNflowTest {
  @Mock
  WorkflowDefinitionService workflowDefinitions;
  @Mock
  WorkflowInstanceService workflowInstances;
  @Mock
  ObjectStringMapper objectMapper;
  @Mock
  WorkflowExecutorListener listener1;
  @Mock
  WorkflowExecutorListener listener2;
  WorkflowExecutorListener[] listeners = new WorkflowExecutorListener[]{listener1, listener2};

  WorkflowExecutorFactory factory;

  @Before
  public void setup() {
    factory = new WorkflowExecutorFactory(workflowDefinitions, workflowInstances, objectMapper);
  }

  @Test
  public void factoryCreatesExecutorsWithoutListeners() {
    WorkflowExecutor executor = factory.createExecutor(12);
    assertNotNull(executor);
  }

  @Test
  public void factoryCreatesExecutorsWithListeners() {
    factory.listeners = listeners;
    WorkflowExecutor executor = factory.createExecutor(122);
    assertNotNull(executor);
  }
}
