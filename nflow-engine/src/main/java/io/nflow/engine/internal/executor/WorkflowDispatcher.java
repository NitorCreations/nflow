package io.nflow.engine.internal.executor;

import static java.lang.Boolean.TRUE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.exception.DispatcherExceptionAnalyzer;
import io.nflow.engine.exception.DispatcherExceptionHandling;
import io.nflow.engine.internal.dao.ExecutorDao;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.util.NflowLogger;
import io.nflow.engine.internal.util.PeriodicLogger;
import io.nflow.engine.service.WorkflowDefinitionService;

@Component
@SuppressFBWarnings(value = "MDM_RANDOM_SEED", justification = "rand does not need to be secure")
public class WorkflowDispatcher implements Runnable {

  private static final Logger logger = getLogger(WorkflowDispatcher.class);
  private static final PeriodicLogger periodicLogger = new PeriodicLogger(logger, 60);

  private final AtomicBoolean shutdownRequested = new AtomicBoolean();
  private final AtomicBoolean running = new AtomicBoolean();
  private final AtomicBoolean paused = new AtomicBoolean();
  private final CountDownLatch shutdownDone = new CountDownLatch(1);

  private final WorkflowInstanceExecutor executor;
  private final WorkflowInstanceDao workflowInstances;
  private final WorkflowStateProcessorFactory stateProcessorFactory;
  private final WorkflowDefinitionService workflowDefinitions;
  private final ExecutorDao executorDao;
  private final DispatcherExceptionAnalyzer exceptionAnalyzer;
  private final NflowLogger nflowLogger;
  private final long sleepTimeMillis;
  private final int stuckThreadThresholdSeconds;
  private final Random rand = new Random();
  private final boolean allowInterrupt;
  private final boolean autoStart;

  @Inject
  public WorkflowDispatcher(WorkflowInstanceExecutor executor, WorkflowInstanceDao workflowInstances,
      WorkflowStateProcessorFactory stateProcessorFactory, WorkflowDefinitionService workflowDefinitions, ExecutorDao executorDao,
      DispatcherExceptionAnalyzer exceptionAnalyzer, NflowLogger nflowLogger, Environment env) {
    this.executor = executor;
    this.workflowInstances = workflowInstances;
    this.stateProcessorFactory = stateProcessorFactory;
    this.workflowDefinitions = workflowDefinitions;
    this.executorDao = executorDao;
    this.exceptionAnalyzer = exceptionAnalyzer;
    this.nflowLogger = nflowLogger;
    this.sleepTimeMillis = env.getRequiredProperty("nflow.dispatcher.sleep.ms", Long.class);
    this.stuckThreadThresholdSeconds = env.getRequiredProperty("nflow.executor.stuckThreadThreshold.seconds", Integer.class);
    this.allowInterrupt = env.getProperty("nflow.executor.interrupt", Boolean.class, TRUE);
    this.autoStart = env.getRequiredProperty("nflow.autostart", Boolean.class);
    if (autoStart) {
      verifyDatabaseSetup();
    }
  }

  @SuppressFBWarnings(value = "WEM_WEAK_EXCEPTION_MESSAGING", justification = "Transaction support exception message is fine")
  private void verifyDatabaseSetup() {
    if (!executorDao.isTransactionSupportEnabled()) {
      throw new BeanCreationException("Transaction support must be enabled");
    }
    if (!executorDao.isAutoCommitEnabled()) {
      throw new BeanCreationException("DataSource must have auto commit enabled");
    }
  }

  @Override
  public void run() {
    try {
      if (!autoStart) {
        verifyDatabaseSetup();
      }
      workflowDefinitions.postProcessWorkflowDefinitions();

      logger.info("Dispatcher started.");
      running.set(true);
      while (!shutdownRequested.get()) {
        if (paused.get()) {
          sleep(false);
        } else {
          try {
            executor.waitUntilQueueSizeLowerThanThreshold(executorDao.getMaxWaitUntil());
            if (!shutdownRequested.get()) {
              if (executorDao.tick()) {
                workflowInstances.recoverWorkflowInstancesFromDeadNodes();
              }
              int potentiallyStuckProcessors = stateProcessorFactory.getPotentiallyStuckProcessors();
              if (potentiallyStuckProcessors > 0) {
                periodicLogger.warn("{} of {} state processor threads are potentially stuck (processing longer than {} seconds)",
                    potentiallyStuckProcessors, executor.getThreadCount(), stuckThreadThresholdSeconds);
              }
              dispatch(getNextInstanceIds());
            }
          } catch (Exception e) {
            DispatcherExceptionHandling handling = exceptionAnalyzer.analyzeSafely(e);
            if (handling.log) {
              if (handling.logStackTrace) {
                StringBuilder sb = new StringBuilder("Exception in executing dispatcher - retrying");
                if (handling.sleep) {
                  sb.append(" after sleep period");
                }
                nflowLogger.log(logger, handling.logLevel, sb.append(" ({})").toString(), new Object[] { e.getMessage(), e });
              } else {
                nflowLogger.log(logger, handling.logLevel, e.getMessage(), new Object[0]);
              }
            }
            if (handling.sleep) {
              sleep(handling.randomizeSleep);
            }
          }
        }
      }
    } finally {
      var graceful = shutdownPool();
      executorDao.markShutdown(graceful);
      running.set(false);
      logger.info("Shutdown completed.");
      shutdownDone.countDown();
    }
  }

  public void shutdown() {
    if (running.get()) {
      if (shutdownRequested.compareAndSet(false, true)) {
        logger.info("Initiating shutdown.");
      }
      try {
        shutdownDone.await();
      } catch (@SuppressWarnings("unused") InterruptedException e) {
        logger.warn("Shutdown interrupted.");
      }
    } else {
      logger.info("Dispatcher was not started or was already shut down.");
    }
  }

  public void pause() {
    paused.set(true);
    logger.info("Dispatcher paused.");
  }

  public void resume() {
    paused.set(false);
    logger.info("Dispatcher resumed.");
  }

  public boolean isPaused() {
    return paused.get();
  }

  public boolean isRunning() {
    return running.get();
  }

  private boolean shutdownPool() {
    try {
      return executor.shutdown(workflowInstances::clearExecutorId, allowInterrupt);
    } catch (Exception e) {
      logger.error("Error in shutting down thread pool.", e);
      return false;
    }
  }

  private void dispatch(List<Long> nextInstanceIds) {
    if (nextInstanceIds.isEmpty()) {
      logger.debug("Found no workflow instances, sleeping.");
      sleep(false);
      return;
    }
    logger.debug("Found {} workflow instances, dispatching executors.", nextInstanceIds.size());
    for (Long instanceId : nextInstanceIds) {
      executor.execute(stateProcessorFactory.createProcessor(instanceId, shutdownRequested::get));
    }
  }

  private List<Long> getNextInstanceIds() {
    int nextBatchSize = executor.getQueueRemainingCapacity();
    logger.debug("Polling next {} workflow instances.", nextBatchSize);
    return workflowInstances.pollNextWorkflowInstanceIds(nextBatchSize);
  }

  @SuppressFBWarnings(value = "MDM_THREAD_YIELD", justification = "Intentionally masking race condition")
  private void sleep(boolean randomize) {
    try {
      if (randomize) {
        Thread.sleep((long) (sleepTimeMillis * rand.nextFloat()));
      } else {
        Thread.sleep(sleepTimeMillis);
      }
    } catch (@SuppressWarnings("unused") InterruptedException ok) {
    }
  }
}
