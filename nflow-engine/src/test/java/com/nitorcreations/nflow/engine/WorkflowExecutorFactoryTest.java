package com.nitorcreations.nflow.engine;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.engine.workflow.data.ObjectStringMapper;

public class WorkflowExecutorFactoryTest extends BaseNflowTest {
  @Mock
  RepositoryService repository;
  @Mock
  ObjectStringMapper objectMapper;
  @Mock
  WorkflowExecutorListener listener1;
  @Mock
  WorkflowExecutorListener listener2;
  List<WorkflowExecutorListener> listeners = asList(listener1, listener2);

  WorkflowExecutorFactory factory;

  @Before
  public void setup() {
    factory = new WorkflowExecutorFactory(repository, objectMapper);
  }

  @Test
  public void factoryCreatesExecutorsWithoutListeners() {
    WorkflowExecutor executor = factory.createExecutor(12);
    assertNotNull(executor);
  }

  @Test
  public void factoryCreatesExecutorsWithListeners() {
    factory.setListeners(listeners);
    WorkflowExecutor executor = factory.createExecutor(122);
    assertNotNull(executor);
  }
}
