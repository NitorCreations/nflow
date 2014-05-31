package com.nitorcreations.nflow.engine;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.RepositoryService;

@Component
public class WorkflowDispatcher implements Runnable {

  private static final Logger logger = getLogger(WorkflowDispatcher.class);

  private volatile boolean shutdownRequested;
  private final CountDownLatch shutdownDone = new CountDownLatch(1);
  private final CongestionControl congestionCtrl;

  private final ThreadPoolTaskExecutor pool;
  private final RepositoryService repository;
  private final WorkflowExecutorFactory executorFactory;
  private final long sleepTime;

  @Inject
  public WorkflowDispatcher(@Named("nflow-executor") ThreadPoolTaskExecutor pool, RepositoryService repository,
      WorkflowExecutorFactory executorFactory, CongestionControl congestionCtrl, Environment env) {
    this.pool = pool;
    this.repository = repository;
    this.executorFactory = executorFactory;
    this.sleepTime = env.getProperty("nflow.dispatcher.sleep.ms", Long.class, 5000l);
    this.congestionCtrl = congestionCtrl;
  }

  @Override
  public void run() {
    logger.info("Starting.");
    try {
      while (!shutdownRequested) {
        try {
          congestionCtrl.waitUntilQueueThreshold();

          if (!shutdownRequested) {
            dispatch(getNextInstanceIds());
          }
        } catch (InterruptedException dropThrough) {
        } catch (Exception e) {
          logger.error("Exception in executing dispatcher - retrying after sleep period.", e);
          sleep();
        }
      }
    } finally {
      shutdownPool();
      logger.info("Shutdown finished.");
      shutdownDone.countDown();
    }
  }

  public void shutdown() {
    shutdownRequested = true;
    logger.info("Shutdown requested.");
    try {
      // TODO use timeout?
      shutdownDone.await();
    } catch (InterruptedException e) {
      logger.info("Shutdown interrupted.");
    }
  }

  private void shutdownPool() {
    try  {
      pool.shutdown();
    } catch (Exception e) {
      logger.error("Error in shutting down thread pool.", e);
    }
  }

  private void dispatch(List<Integer> nextInstanceIds) {
    if (nextInstanceIds.isEmpty()) {
      logger.debug("Found no workflow instances, sleeping.");
      sleep();
      return;
    }

    logger.debug("Found {} workflow instances, dispatching executors.", nextInstanceIds.size());
    for (Integer instanceId : nextInstanceIds) {
      congestionCtrl.register(pool.submitListenable(executorFactory.createExecutor(instanceId)));
    }
  }

  private List<Integer> getNextInstanceIds() {
    int nextBatchSize = Math.max(0, 2 * pool.getMaxPoolSize() - pool.getActiveCount());
    logger.debug("Polling next {} workflow instances.", nextBatchSize);
    return repository.pollNextWorkflowInstanceIds(nextBatchSize);
  }

  private void sleep() {
    try {
      Thread.sleep(sleepTime);
    } catch (InterruptedException ok) {
    }
  }
}
