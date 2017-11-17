package io.nflow.engine.internal.config;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.nflow.engine.internal.executor.WorkflowDispatcher;
import io.nflow.engine.service.WorkflowDefinitionService;

@Component
public class WorkflowLifecycle implements SmartLifecycle {
  private static final Logger logger = getLogger(WorkflowLifecycle.class);

  private final WorkflowDefinitionService workflowDefinitions;
  private final WorkflowDispatcher dispatcher;
  private final boolean autoStart;
  private final Thread dispatcherThread;

  @Inject
  public WorkflowLifecycle(WorkflowDefinitionService workflowDefinitions, WorkflowDispatcher dispatcher,
      @NFlow ThreadFactory nflowThreadFactory, Environment env) throws IOException, ReflectiveOperationException {
    this.dispatcher = dispatcher;
    this.workflowDefinitions = workflowDefinitions;
    if (env.getRequiredProperty("nflow.autoinit", Boolean.class)) {
      this.workflowDefinitions.postProcessWorkflowDefinitions();
    } else {
      logger.info("nFlow engine autoinit disabled (system property nflow.autoinit=false)");
    }
    autoStart = env.getRequiredProperty("nflow.autostart", Boolean.class);
    dispatcherThread = nflowThreadFactory.newThread(dispatcher);
    dispatcherThread.setName("nflow-dispatcher");
    if (!autoStart) {
      logger.info("nFlow engine autostart disabled (system property nflow.autostart=false)");
    }
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }

  @Override
  public boolean isAutoStartup() {
    return autoStart;
  }

  @Override
  public void start() {
    dispatcherThread.start();
  }

  @Override
  public boolean isRunning() {
    return dispatcherThread.isAlive();
  }

  @Override
  public void stop() {
    dispatcher.shutdown();
  }

  @Override
  public void stop(Runnable callback) {
    stop();
    callback.run();
  }
}
