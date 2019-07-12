package io.nflow.engine.internal.executor;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ThreadFactory;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.nflow.engine.config.NFlow;

@Component
public class WorkflowLifecycle implements SmartLifecycle {
  private static final Logger logger = getLogger(WorkflowLifecycle.class);

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
