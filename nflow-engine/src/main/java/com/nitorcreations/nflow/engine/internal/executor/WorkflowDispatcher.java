package com.nitorcreations.nflow.engine.internal.executor;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.dao.ExecutorDao;
import com.nitorcreations.nflow.engine.internal.dao.PollingRaceConditionException;
import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;

@Component
public class WorkflowDispatcher implements Runnable {

  private static final Logger logger = getLogger(WorkflowDispatcher.class);

  private volatile boolean shutdownRequested;
  private final CountDownLatch shutdownDone = new CountDownLatch(1);

  private final ThresholdThreadPoolTaskExecutor pool;
  private final WorkflowInstanceDao workflowInstances;
  private final WorkflowExecutorFactory executorFactory;
  private final ExecutorDao executorRecovery;
  private final long sleepTime;
  private final Random rand = new Random();

  @Inject
  public WorkflowDispatcher(@Named("nflowExecutor") ThresholdThreadPoolTaskExecutor pool, WorkflowInstanceDao workflowInstances,
      WorkflowExecutorFactory executorFactory, ExecutorDao executorRecovery, Environment env) {
    this.pool = pool;
    this.workflowInstances = workflowInstances;
    this.executorFactory = executorFactory;
    this.executorRecovery = executorRecovery;
    this.sleepTime = env.getProperty("nflow.dispatcher.sleep.ms", Long.class, 5000l);
  }

  @Override
  public void run() {
    logger.info("Starting.");
    try {
      while (!shutdownRequested) {
        try {
          pool.waitUntilQueueSizeLowerThanThreshold(executorRecovery.getMaxWaitUntil());

          if (!shutdownRequested) {
            executorRecovery.tick();
            dispatch(getNextInstanceIds());
          }
        } catch (PollingRaceConditionException pex) {
          logger.info(pex.getMessage());
          sleep(true);
        } catch (InterruptedException dropThrough) {
        } catch (Exception e) {
          logger.error("Exception in executing dispatcher - retrying after sleep period (" + e.getMessage() + ")", e);
          sleep(false);
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
      sleep(false);
      return;
    }

    logger.debug("Found {} workflow instances, dispatching executors.", nextInstanceIds.size());
    for (Integer instanceId : nextInstanceIds) {
      pool.submit(executorFactory.createExecutor(instanceId));
    }
  }

  private List<Integer> getNextInstanceIds() {
    int nextBatchSize = Math.max(0, 2 * pool.getMaxPoolSize() - pool.getActiveCount());
    logger.debug("Polling next {} workflow instances.", nextBatchSize);
    return workflowInstances.pollNextWorkflowInstanceIds(nextBatchSize);
  }

  private void sleep(boolean randomize) {
    try {
      if (randomize) {
        Thread.sleep((long)(sleepTime * rand.nextDouble()));
      } else {
        Thread.sleep(sleepTime);
      }
    } catch (InterruptedException ok) {
    }
  }
}
