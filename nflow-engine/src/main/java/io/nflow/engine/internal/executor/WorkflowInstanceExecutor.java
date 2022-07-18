package io.nflow.engine.internal.executor;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

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

  public void execute(Runnable runnable) {
    executor.execute(runnable);
  }

  public int getQueueRemainingCapacity() {
    return queue.remainingCapacity();
  }

  public boolean shutdown() {
    var lessGracefulTimeout = min(5000, SECONDS.toMillis(awaitTerminationSeconds) / 3);
    executor.shutdown();
    boolean allProcessed = true;
    try {
      if (!executor.awaitTermination(awaitTerminationSeconds - lessGracefulTimeout, MILLISECONDS)) {
        logger.warn("Graceful shutdown timed out while waiting for executor queue to drain");
        var tasksToMarkReadyForExecuting = executor.shutdownNow();
        allProcessed = tasksToMarkReadyForExecuting.isEmpty();
        // TODO: loop through the tasks and mark their executor_id to null, they never started running so no action needs to be inserted
        if (!executor.awaitTermination(lessGracefulTimeout, MILLISECONDS)) {
          logger.warn("Hard shutdown timed out while waiting running workflows to interrupt and finish");
        }
      }
    } catch (@SuppressWarnings("unused") InterruptedException ex) {
      logger.warn("Interrupted while waiting for executor to terminate");
      currentThread().interrupt();
    }
    var graceful = executor.isTerminated() && allProcessed;
    if (graceful) {
      logger.info("Graceful shutdown succeeded");
    }
    return graceful;
  }
}
