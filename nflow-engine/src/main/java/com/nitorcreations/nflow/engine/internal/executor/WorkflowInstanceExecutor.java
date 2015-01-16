package com.nitorcreations.nflow.engine.internal.executor;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import org.joda.time.DateTime;
import org.slf4j.Logger;

public class WorkflowInstanceExecutor {
  private static final Logger logger = getLogger(WorkflowInstanceExecutor.class);

  private final int awaitTerminationSeconds;
  final ThreadPoolExecutor executor;
  final ThresholdBlockingQueue<Runnable> queue;

  public WorkflowInstanceExecutor(int maxQueueSize, int threadCount, int notifyThreshold, int awaitTerminationSeconds,
      int keepAliveSeconds,
      ThreadFactory threadFactory) {
    queue = new ThresholdBlockingQueue<>(maxQueueSize, notifyThreshold);
    executor = new ThreadPoolExecutor(threadCount, threadCount, keepAliveSeconds, SECONDS, queue, threadFactory);
    executor.allowCoreThreadTimeOut(keepAliveSeconds > 0);
    this.awaitTerminationSeconds = awaitTerminationSeconds;
  }

  public void waitUntilQueueSizeLowerThanThreshold(DateTime waitUntil) throws InterruptedException {
    queue.waitUntilQueueSizeLowerThanThreshold(waitUntil);
  }

  public void execute(Runnable runnable) {
    executor.execute(runnable);
  }

  public int getQueueRemainingCapacity() {
    return queue.remainingCapacity();
  }

  public void shutdown() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(awaitTerminationSeconds, SECONDS)) {
        logger.warn("Timed out while waiting for executor to terminate");
      }
    } catch (InterruptedException ex) {
      logger.warn("Interrupted while waiting for executor to terminate");
      currentThread().interrupt();
    }
  }
}
