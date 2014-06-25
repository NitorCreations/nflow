package com.nitorcreations.nflow.engine;

import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;

import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class WorkflowLifecycle implements SmartLifecycle {
  private final WorkflowDispatcher dispatcher;
  private final boolean autoStart;
  private final Thread dispatcherThread;

  @Inject
  public WorkflowLifecycle(WorkflowDispatcher dispatcher, Environment env) {
    this.dispatcher = dispatcher;
    this.autoStart = env.getProperty("nflow.autostart", Boolean.class, true);
    this.dispatcherThread = new Thread(dispatcher, "nflow-dispatcher");
    if (!this.autoStart) {
      getLogger(WorkflowLifecycle.class).info("nFlow engine autostart disabled (system property nflow.autostart=false)");
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
    this.dispatcherThread.start();
  }

  @Override
  public boolean isRunning() {
    return dispatcherThread.isAlive();
  }

  @Override
  public void stop() {
    throw new IllegalStateException("This method is never called by spring");
  }

  @Override
  public void stop(Runnable callback) {
    this.dispatcher.shutdown();
    callback.run();
  }
}
