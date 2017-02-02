package io.nflow.engine.internal.executor;

import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.mock.env.MockEnvironment;

import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import io.nflow.engine.listener.WorkflowExecutorListener;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.service.WorkflowInstanceService;

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
  WorkflowExecutorListener[] listeners = new WorkflowExecutorListener[] { listener1, listener2 };
  WorkflowStateProcessorFactory factory;
  private static final int STUCK_THREAD_THRESHOLD = 5;

  @Before
  public void setup() {
    env.setProperty("nflow.illegal.state.change.action", "ignore");
    env.setProperty("nflow.unknown.workflow.type.retry.delay.minutes", "60");
    env.setProperty("nflow.unknown.workflow.state.retry.delay.minutes", "60");
    env.setProperty("nflow.executor.stuckThreadThreshold.seconds", Integer.toString(STUCK_THREAD_THRESHOLD));
    factory = new WorkflowStateProcessorFactory(workflowDefinitions, workflowInstances, objectMapper, workflowInstanceDao,
        workflowInstancePreProcessor, env);
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

  @Test
  public void checkIfStateProcessorsAreStuckLogsLongRunningInstance() {
    WorkflowStateProcessor executor1 = mock(WorkflowStateProcessor.class);
    WorkflowStateProcessor executor2 = mock(WorkflowStateProcessor.class);
    when(executor1.getStartTimeSeconds()).thenReturn(currentTimeMillis() / 1000 - STUCK_THREAD_THRESHOLD - 1);
    when(executor2.getStartTimeSeconds()).thenReturn(currentTimeMillis() / 1000 - STUCK_THREAD_THRESHOLD);
    factory.processingInstances.put(111, executor1);
    factory.processingInstances.put(222, executor2);

    int potentiallyStuckProcessors = factory.getPotentiallyStuckProcessors();

    assertThat(potentiallyStuckProcessors, is(1));
    verify(executor1).logPotentiallyStuck(anyLong());
    verify(executor2, never()).logPotentiallyStuck(anyLong());
  }
}
