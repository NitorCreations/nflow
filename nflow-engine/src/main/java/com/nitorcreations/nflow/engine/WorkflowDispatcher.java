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

  private final ThreadPoolTaskExecutor pool;
  private final RepositoryService repository;
  private final WorkflowExecutorFactory executorFactory;
  private final long sleepTime;

  @Inject
  public WorkflowDispatcher(@Named("nflow-executor") ThreadPoolTaskExecutor pool, RepositoryService repository,
      WorkflowExecutorFactory executorFactory, Environment env) {
    this.pool = pool;
    this.repository = repository;
    this.executorFactory = executorFactory;
    this.sleepTime = env.getProperty("nflow.dispatcher.sleep.ms", Long.class, 5000l);
  }

  @Override
  public void run() {
    logger.info("Starting.");
    try {
      while (!shutdownRequested) {
        try {
          List<Integer> nextInstanceIds = getNextInstanceIds();
          if (nextInstanceIds.isEmpty()) {
            logger.debug("Found no workflow instances, sleeping.");
            sleep();
          } else {
            dispatchWorkflows(nextInstanceIds);
          }
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

  private List<Integer> getNextInstanceIds() {
    int nextBatchSize = calculateBatchSize();
    logger.debug("Polling next {} workflow instances.", nextBatchSize);
    return repository.pollNextWorkflowInstanceIds(nextBatchSize);
  }

  private int calculateBatchSize() {
    return Math.max(0, 2 * pool.getMaxPoolSize() - pool.getActiveCount());
  }

  private void dispatchWorkflows(List<Integer> instanceIds) {
    logger.debug("Found {} workflow instances, dispatching executors.", instanceIds.size());
    for (Integer instanceId : instanceIds) {
      pool.execute(executorFactory.createExecutor(instanceId));
    }
  }

  private void sleep() {
    try {
      Thread.sleep(sleepTime);
    } catch (InterruptedException ok) {
    }
  }
}
