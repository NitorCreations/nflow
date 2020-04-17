package io.nflow.engine.internal.executor;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mock.env.MockEnvironment;

import io.nflow.engine.internal.dao.MaintenanceDao;
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
  MaintenanceDao maintenanceDao;
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

  @BeforeEach
  public void setup() {
    env.setProperty("nflow.illegal.state.change.action", "ignore");
    env.setProperty("nflow.unknown.workflow.type.retry.delay.minutes", "60");
    env.setProperty("nflow.unknown.workflow.state.retry.delay.minutes", "60");
    env.setProperty("nflow.executor.stuckThreadThreshold.seconds", Integer.toString(STUCK_THREAD_THRESHOLD));
    env.setProperty("nflow.executor.stateProcessingRetryDelay.seconds", "1");
    env.setProperty("nflow.executor.stateSaveRetryDelay.seconds", "60");
    env.setProperty("nflow.executor.stateVariableValueTooLongRetryDelay.minutes", "60");
    env.setProperty("nflow.db.workflowInstanceType.cacheSize", "10000");
    factory = new WorkflowStateProcessorFactory(workflowDefinitions, workflowInstances, objectMapper, workflowInstanceDao,
        maintenanceDao, workflowInstancePreProcessor, env);
  }

  @Test
  public void factoryCreatesExecutorsWithoutListeners() {
    WorkflowStateProcessor executor = factory.createProcessor(12, FALSE::booleanValue);
    assertNotNull(executor);
  }

  @Test
  public void factoryCreatesExecutorsWithListeners() {
    factory.listeners = listeners;
    WorkflowStateProcessor executor = factory.createProcessor(122, FALSE::booleanValue);
    assertNotNull(executor);
  }

  @Test
  public void checkIfStateProcessorsAreStuckLogsLongRunningInstance() {
    WorkflowStateProcessor executor1 = mock(WorkflowStateProcessor.class);
    WorkflowStateProcessor executor2 = mock(WorkflowStateProcessor.class);
    when(executor1.getStartTimeSeconds()).thenReturn(currentTimeMillis() / 1000 - STUCK_THREAD_THRESHOLD - 1);
    when(executor2.getStartTimeSeconds()).thenReturn(currentTimeMillis() / 1000 - STUCK_THREAD_THRESHOLD);
    factory.processingInstances.put(111L, executor1);
    factory.processingInstances.put(222L, executor2);

    int potentiallyStuckProcessors = factory.getPotentiallyStuckProcessors();

    assertThat(potentiallyStuckProcessors, is(1));
    verify(executor1).logPotentiallyStuck(anyLong());
    verify(executor2, never()).logPotentiallyStuck(anyLong());
  }
}
