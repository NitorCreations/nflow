package io.nflow.engine.internal.executor;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import org.joda.time.DateTime;
import org.slf4j.Logger;

public class WorkflowInstanceExecutor {
  private static final Logger logger = getLogger(WorkflowInstanceExecutor.class);

  private final int awaitTerminationSeconds;
  private final int threadCount;
  final ThreadPoolExecutor executor;
  final ThresholdBlockingQueue<Runnable> queue;

  public WorkflowInstanceExecutor(int maxQueueSize, int threadCount, int notifyThreshold, int awaitTerminationSeconds,
      int keepAliveSeconds,
      ThreadFactory threadFactory) {
    queue = new ThresholdBlockingQueue<>(maxQueueSize, notifyThreshold);
    executor = new ThreadPoolExecutor(threadCount, threadCount, keepAliveSeconds, SECONDS, queue, threadFactory);
    executor.allowCoreThreadTimeOut(keepAliveSeconds > 0);
    this.awaitTerminationSeconds = awaitTerminationSeconds;
    this.threadCount = threadCount;
  }

  public int getThreadCount() {
    return threadCount;
  }

  public void waitUntilQueueSizeLowerThanThreshold(DateTime waitUntil) throws InterruptedException {
    queue.waitUntilQueueSizeLowerThanThreshold(waitUntil);
  }

  public void wakeUpDispatcherIfNeeded() {
    queue.notifyIfNotFull();
  }

  public void execute(WorkflowStateProcessor runnable) {
    executor.execute(runnable);
  }

  public int getQueueRemainingCapacity() {
    return queue.remainingCapacity();
  }

  public boolean shutdown(Consumer<List<WorkflowStateProcessor>> queuedWorkflowRecovery) {
    var fullTimeout = SECONDS.toMillis(awaitTerminationSeconds);
    var lessGracefulTimeout = min(5000, fullTimeout / 3);
    var gracefulTimeout = fullTimeout - lessGracefulTimeout;
    // step 1: stop accepting new jobs
    executor.shutdown();
    // step 2: drain the queue that has not yet started processing and mark their executorId to null
    //         which makes them immediately runnable by other executors
    List<Runnable> tasksToMarkReadyForExecuting = new ArrayList<>();
    queue.drainTo(tasksToMarkReadyForExecuting);
    recoverQueuedWorkflows(tasksToMarkReadyForExecuting, queuedWorkflowRecovery);

    try {
      // step 3: wait for graceful time for in progress to stop
      if (!executor.awaitTermination(gracefulTimeout, MILLISECONDS)) {
        logger.warn("Graceful shutdown timed out after {}s while waiting for executor queue to drain", (gracefulTimeout+999)/1000);
        // step 4: send thread interrupt to the in progress workflows and wait for them to stop for less graceful timeout
        //         if any workflows were still in the queue then mark their executorId to null
        tasksToMarkReadyForExecuting.addAll(executor.shutdownNow());
        recoverQueuedWorkflows(tasksToMarkReadyForExecuting, queuedWorkflowRecovery);
        if (!executor.awaitTermination(lessGracefulTimeout, MILLISECONDS)) {
          logger.warn("Hard shutdown timed out after {}s while waiting running workflows to interrupt and finish", (lessGracefulTimeout+999)/1000);
        }
      }
    } catch (@SuppressWarnings("unused") InterruptedException ex) {
      logger.warn("Interrupted while waiting for executor to terminate");
      currentThread().interrupt();
    }
    // step 5: success if the thread pool is fully terminated and there are no tasks that have not been reset
    var graceful = executor.isTerminated() && tasksToMarkReadyForExecuting.isEmpty();
    if (graceful) {
      logger.info("Graceful shutdown succeeded");
    }
    return graceful;
  }

  private void recoverQueuedWorkflows(List<Runnable> queuedWorkflows, Consumer<List<WorkflowStateProcessor>> queuedWorkflowRecovery) {
    if (queuedWorkflows.isEmpty()) {
      return;
    }
    @SuppressWarnings("unchecked")
    var tasksToMarkReadyForExecuting = (List<WorkflowStateProcessor>) (Object) queuedWorkflows;
    queuedWorkflowRecovery.accept(tasksToMarkReadyForExecuting);
  }
}
