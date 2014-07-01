package com.nitorcreations.nflow.engine.internal.config;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ThreadFactory;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.executor.WorkflowDispatcher;

@Component
public class WorkflowLifecycle implements SmartLifecycle {
  private final WorkflowDispatcher dispatcher;
  private final boolean autoStart;
  private final Thread dispatcherThread;

  @Inject
  public WorkflowLifecycle(WorkflowDispatcher dispatcher, @Named("nflow-ThreadFactory") ThreadFactory threadFactory, Environment env) {
    this.dispatcher = dispatcher;
    this.autoStart = env.getProperty("nflow.autostart", Boolean.class, true);
    this.dispatcherThread = threadFactory.newThread(dispatcher);
    this.dispatcherThread.setName("nflow-dispatcher");
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
