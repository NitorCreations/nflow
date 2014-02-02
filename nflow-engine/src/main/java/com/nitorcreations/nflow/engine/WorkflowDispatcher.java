package com.nitorcreations.nflow.engine;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.RepositoryService;

@Component
public class WorkflowDispatcher implements Runnable {

  private static final Logger logger = getLogger(WorkflowDispatcher.class);

  private volatile boolean shutdownFlag;

  private final ThreadPoolTaskExecutor pool;
  private final RepositoryService repository;
  private final WorkflowExecutorFactory executorFactory;
  private final long sleepTime;

  @Inject
  public WorkflowDispatcher(ThreadPoolTaskExecutor pool, RepositoryService repository, WorkflowExecutorFactory executorFactory,
      Environment env) {
    this.pool = pool;
    this.repository = repository;
    this.executorFactory = executorFactory;
    this.sleepTime = env.getProperty("dispatcher.sleep.ms", Long.class, 5000l);
  }

  public void run() {
    logger.info("Starting.");
    try {
      while (!shutdownFlag) {
        try {
          int nextBatchSize = Math.max(0, 2 * pool.getMaxPoolSize() - pool.getActiveCount());
          logger.debug("Polling next " + nextBatchSize + " workflow instances");
          List<Integer> nextInstanceIds = repository.pollNextWorkflowInstanceIds(nextBatchSize);
          if (nextInstanceIds.isEmpty()) {
            logger.debug("Found no workflow instances, sleeping.");
            sleep();
          } else {
            logger.debug("Found " + nextInstanceIds.size() + " workflow instances, dispatching executors.");
            for (Integer instanceId : nextInstanceIds) {
              WorkflowExecutor executor = executorFactory.createExecutor(instanceId);
              pool.execute(executor);
            }
          }
        } catch (Exception ex) {
          logger.error("Exception in executing dispatcher - retrying after sleep period.", ex);
          sleep();
        }
      }
    } finally {
      logger.info("Shutdown finished.");
    }
  }

  private void sleep() {
    try {
      Thread.sleep(sleepTime);
    } catch (InterruptedException e) {
      /* ok */
    }
  }

  public void shutdown() {
    this.shutdownFlag = true;
    logger.info("Shutdown starting.");
    try {
      pool.shutdown();
    } catch (Exception ex) {
      logger.error("Error in shutting down thread pool", ex);
    }
  }

}
