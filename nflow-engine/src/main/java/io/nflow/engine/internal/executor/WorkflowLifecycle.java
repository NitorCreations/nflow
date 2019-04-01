package io.nflow.engine.internal.executor;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.nflow.engine.config.NFlow;
import io.nflow.engine.service.WorkflowDefinitionService;

@Component
public class WorkflowLifecycle implements SmartLifecycle {
  private static final Logger logger = getLogger(WorkflowLifecycle.class);

  private final WorkflowDefinitionService workflowDefinitions;
  private final WorkflowDispatcher dispatcher;
  private final ThreadFactory nflowThreadFactory;
  private final boolean autoStart;
  private volatile Thread dispatcherThread;

  @Inject
  public WorkflowLifecycle(WorkflowDefinitionService workflowDefinitions, WorkflowDispatcher dispatcher,
      @NFlow ThreadFactory nflowThreadFactory, Environment env) throws IOException, ReflectiveOperationException {
    this.dispatcher = dispatcher;
    this.nflowThreadFactory = nflowThreadFactory;
    this.workflowDefinitions = workflowDefinitions;
    if (env.getRequiredProperty("nflow.autoinit", Boolean.class)) {
      this.workflowDefinitions.postProcessWorkflowDefinitions();
    } else {
      logger.info("nFlow engine autoinit disabled (system property nflow.autoinit=false)");
    }
    autoStart = env.getRequiredProperty("nflow.autostart", Boolean.class);
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
  public synchronized void start() {
    if (dispatcherThread == null) {
      dispatcherThread = createDispatcherThread();
      dispatcherThread.start();
    }
  }

  public void pause() {
    dispatcher.pause();
  }

  public void resume() {
    dispatcher.resume();
  }

  public boolean isPaused() {
    return dispatcher.isPaused();
  }

  @Override
  public boolean isRunning() {
    return dispatcherThread != null && dispatcherThread.isAlive();
  }

  @Override
  public synchronized void stop() {
    dispatcher.shutdown();
    dispatcherThread = null;
  }

  @Override
  public void stop(Runnable callback) {
    stop();
    callback.run();
  }

  private Thread createDispatcherThread() {
    final Thread thread = nflowThreadFactory.newThread(dispatcher);
    thread.setName("nflow-dispatcher");
    if (!autoStart) {
      logger.info("nFlow engine autostart disabled (system property nflow.autostart=false)");
    }
    return thread;
  }
}
