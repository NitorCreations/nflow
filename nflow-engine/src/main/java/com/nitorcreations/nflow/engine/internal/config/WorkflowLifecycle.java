package com.nitorcreations.nflow.engine.internal.config;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ThreadFactory;

import javax.inject.Inject;

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
  public WorkflowLifecycle(WorkflowDispatcher dispatcher, @NFlow ThreadFactory nflowThreadFactory, Environment env) {
    this.dispatcher = dispatcher;
    autoStart = env.getRequiredProperty("nflow.autostart", Boolean.class);
    dispatcherThread = nflowThreadFactory.newThread(dispatcher);
    dispatcherThread.setName("nflow-dispatcher");
    if (!autoStart) {
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
