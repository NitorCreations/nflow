package com.nitorcreations.nflow.engine.internal.executor;

import static org.junit.Assert.assertNotNull;

import com.nitorcreations.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.mock.env.MockEnvironment;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;

public class WorkflowStateProcessorFactoryTest extends BaseNflowTest {
  @Mock
  WorkflowDefinitionService workflowDefinitions;
  @Mock
  WorkflowInstanceService workflowInstances;
  @Mock
  ObjectStringMapper objectMapper;
  @Mock
  WorkflowInstanceDao workflowInstanceDao;
  @Mock
  WorkflowInstancePreProcessor workflowInstancePreProcessor;
  MockEnvironment env = new MockEnvironment();
  @Mock
  WorkflowExecutorListener listener1;
  @Mock
  WorkflowExecutorListener listener2;
  WorkflowExecutorListener[] listeners = new WorkflowExecutorListener[]{listener1, listener2};

  WorkflowStateProcessorFactory factory;

  @Before
  public void setup() {
    env.setProperty("nflow.illegal.state.change.action", "ignore");
    env.setProperty("nflow.unknown.workflow.type.retry.delay.minutes", "60");
    env.setProperty("nflow.unknown.workflow.state.retry.delay.minutes", "60");
    factory = new WorkflowStateProcessorFactory(workflowDefinitions, workflowInstances, objectMapper,
            workflowInstanceDao, workflowInstancePreProcessor, env);
  }

  @Test
  public void factoryCreatesExecutorsWithoutListeners() {
    WorkflowStateProcessor executor = factory.createProcessor(12);
    assertNotNull(executor);
  }

  @Test
  public void factoryCreatesExecutorsWithListeners() {
    factory.listeners = listeners;
    WorkflowStateProcessor executor = factory.createProcessor(122);
    assertNotNull(executor);
  }
}
