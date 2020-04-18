package io.nflow.engine.internal.executor;

import static io.nflow.engine.internal.executor.WorkflowDispatcher.Status.finished;
import static io.nflow.engine.internal.executor.WorkflowDispatcher.Status.notStarted;
import static io.nflow.engine.internal.executor.WorkflowDispatcher.Status.running;
import static io.nflow.engine.internal.executor.WorkflowDispatcher.Status.shuttingDown;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.dao.ExecutorDao;
import io.nflow.engine.internal.dao.PollingBatchException;
import io.nflow.engine.internal.dao.PollingRaceConditionException;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.util.PeriodicLogger;
import io.nflow.engine.service.WorkflowDefinitionService;

@Component
@SuppressFBWarnings(value = "MDM_RANDOM_SEED", justification = "rand does not need to be secure")
public class WorkflowDispatcher implements Runnable {

  private static final Logger logger = getLogger(WorkflowDispatcher.class);
  private static final PeriodicLogger periodicLogger = new PeriodicLogger(logger, 60);

  private final AtomicReference<Status> status = new AtomicReference<>(notStarted);
  private final AtomicBoolean paused = new AtomicBoolean();
  private final CountDownLatch shutdownDone = new CountDownLatch(1);

  private final WorkflowInstanceExecutor executor;
  private final WorkflowInstanceDao workflowInstances;
  private final WorkflowStateProcessorFactory stateProcessorFactory;
  private final WorkflowDefinitionService workflowDefinitions;
  private final ExecutorDao executorDao;
  private final long sleepTimeMillis;
  private final int stuckThreadThresholdSeconds;
  private final Random rand = new Random();

  enum Status {
    notStarted(false, false), running(true, false), shuttingDown(true, true), finished(false, true);

    public final boolean isRunning;
    public final boolean isShutdownRequested;

    Status(boolean isRunning, boolean isShutdownRequested) {
      this.isRunning = isRunning;
      this.isShutdownRequested = isShutdownRequested;
    }
  }

  @Inject
  @SuppressFBWarnings(value = "WEM_WEAK_EXCEPTION_MESSAGING", justification = "Transaction support exception message is fine")
  public WorkflowDispatcher(WorkflowInstanceExecutor executor, WorkflowInstanceDao workflowInstances,
      WorkflowStateProcessorFactory stateProcessorFactory, WorkflowDefinitionService workflowDefinitions, ExecutorDao executorDao,
      Environment env) {
    this.executor = executor;
    this.workflowInstances = workflowInstances;
    this.stateProcessorFactory = stateProcessorFactory;
    this.workflowDefinitions = workflowDefinitions;
    this.executorDao = executorDao;
    this.sleepTimeMillis = env.getRequiredProperty("nflow.dispatcher.sleep.ms", Long.class);
    this.stuckThreadThresholdSeconds = env.getRequiredProperty("nflow.executor.stuckThreadThreshold.seconds", Integer.class);

    if (!executorDao.isTransactionSupportEnabled()) {
      throw new BeanCreationException("Transaction support must be enabled");
    }
    if (!executorDao.isAutoCommitEnabled()) {
      throw new BeanCreationException("DataSource must have auto commit enabled");
    }
  }

  @Override
  public void run() {
    logger.info("Dispacther started.");
    try {
      status.set(running);
      workflowDefinitions.postProcessWorkflowDefinitions();
      while (status.get() == running) {
        if (paused.get()) {
          sleep(false);
        } else {
          try {
            executor.waitUntilQueueSizeLowerThanThreshold(executorDao.getMaxWaitUntil());
            if (status.get() == running) {
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
          } catch (PollingRaceConditionException pex) {
            logger.debug(pex.getMessage());
            sleep(true);
          } catch (PollingBatchException pex) {
            logger.warn(pex.getMessage());
          } catch (@SuppressWarnings("unused") InterruptedException dropThrough) {
          } catch (Exception e) {
            logger.error("Exception in executing dispatcher - retrying after sleep period (" + e.getMessage() + ")", e);
            sleep(false);
          }
        }
      }
    } finally {
      shutdownPool();
      executorDao.markShutdown();
      shutdownDone.countDown();
    }
  }

  public void shutdown() {
    switch (status.get()) {
    case notStarted:
    case finished:
      logger.info("Dispatcher was not started or was already shut down.");
      return;
    case running:
      if (status.compareAndSet(running, shuttingDown)) {
        logger.info("Shutdown initiated.");
      }
      break;
    default:
      break;
    }
    try {
      shutdownDone.await();
      if (status.compareAndSet(shuttingDown, finished)) {
        logger.info("Shutdown completed.");
      }
    } catch (@SuppressWarnings("unused") InterruptedException e) {
      logger.warn("Shutdown interrupted.");
    }
  }

  public void pause() {
    if (paused.compareAndSet(false, true)) {
      logger.info("Dispatcher paused.");
    }
  }

  public void resume() {
    if (paused.compareAndSet(true, false)) {
      logger.info("Dispatcher resumed.");
    }
  }

  public boolean isPaused() {
    return paused.get();
  }

  public boolean isRunning() {
    return status.get().isRunning;
  }

  private void shutdownPool() {
    try {
      executor.shutdown();
    } catch (Exception e) {
      logger.error("Error in shutting down thread pool.", e);
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
      executor.execute(stateProcessorFactory.createProcessor(instanceId, () -> status.get().isShutdownRequested));
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
