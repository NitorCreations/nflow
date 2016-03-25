package com.nitorcreations.nflow.engine.internal.executor;

import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.mock.env.MockEnvironment;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

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
  @Mock
  private Appender<ILoggingEvent> mockAppender;
  @Captor
  private ArgumentCaptor<ILoggingEvent> loggingEventCaptor;
  WorkflowExecutorListener[] listeners = new WorkflowExecutorListener[] { listener1, listener2 };
  WorkflowStateProcessorFactory factory;

  @Before
  public void setup() {
    env.setProperty("nflow.illegal.state.change.action", "ignore");
    env.setProperty("nflow.unknown.workflow.type.retry.delay.minutes", "60");
    env.setProperty("nflow.unknown.workflow.state.retry.delay.minutes", "60");
    factory = new WorkflowStateProcessorFactory(workflowDefinitions, workflowInstances, objectMapper, workflowInstanceDao,
        workflowInstancePreProcessor, env);
    Logger logger = (Logger) getLogger(ROOT_LOGGER_NAME);
    logger.addAppender(mockAppender);
  }

  @After
  public void teardown() {
    Logger logger = (Logger) getLogger(ROOT_LOGGER_NAME);
    logger.detachAppender(mockAppender);
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
    factory.processingInstances.put(111, now().minusMinutes(6));
    factory.processingInstances.put(222, now().minusMinutes(5));

    factory.getPotentiallyStuckProcessors();

    verify(mockAppender).doAppend(loggingEventCaptor.capture());
    ILoggingEvent event = loggingEventCaptor.getValue();
    assertThat(event.getLevel(), is(Level.WARN));
    assertThat(event.getFormattedMessage(), is("Workflow instance 111 has been processed for 6 minutes, it may be stuck."));
  }
}
