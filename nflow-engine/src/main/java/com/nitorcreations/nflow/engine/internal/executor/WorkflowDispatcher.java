package com.nitorcreations.nflow.engine.internal.executor;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
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

  private final WorkflowInstanceExecutor executor;
  private final WorkflowInstanceDao workflowInstances;
  private final WorkflowStateProcessorFactory stateProcessorFactory;
  private final ExecutorDao executorRecovery;
  private final long sleepTime;
  private final Random rand = new Random();

  @Inject
  public WorkflowDispatcher(WorkflowInstanceExecutor executor, WorkflowInstanceDao workflowInstances,
      WorkflowStateProcessorFactory stateProcessorFactory, ExecutorDao executorRecovery, Environment env) {
    this.executor = executor;
    this.workflowInstances = workflowInstances;
    this.stateProcessorFactory = stateProcessorFactory;
    this.executorRecovery = executorRecovery;
    this.sleepTime = env.getRequiredProperty("nflow.dispatcher.sleep.ms", Long.class);
    if (!executorRecovery.isTransactionSupportEnabled()) {
      throw new BeanCreationException("Transaction support must be enabled");
    }
  }

  @Override
  public void run() {
    logger.info("Starting.");
    try {
      while (!shutdownRequested) {
        try {
          executor.waitUntilQueueSizeLowerThanThreshold(executorRecovery.getMaxWaitUntil());

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
      executor.shutdown();
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
      executor.execute(stateProcessorFactory.createProcessor(instanceId));
    }
  }

  private List<Integer> getNextInstanceIds() {
    int nextBatchSize = executor.getQueueRemainingCapacity();
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
