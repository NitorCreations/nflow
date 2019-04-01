package io.nflow.engine.internal.executor;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import org.joda.time.DateTime;
import org.slf4j.Logger;

public class WorkflowInstanceExecutor {
  private static final Logger logger = getLogger(WorkflowInstanceExecutor.class);

  private volatile boolean needsInitialization = true;
  private final int awaitTerminationSeconds;
  private final int maxQueueSize;
  private final int threadCount;
  private final int notifyThreshold;
  private final int keepAliveSeconds;
  private final ThreadFactory threadFactory;
  ThreadPoolExecutor executor;
  ThresholdBlockingQueue<Runnable> queue;

  public WorkflowInstanceExecutor(int maxQueueSize, int threadCount, int notifyThreshold, int awaitTerminationSeconds,
      int keepAliveSeconds,
      ThreadFactory threadFactory) {
    this.awaitTerminationSeconds = awaitTerminationSeconds;
    this.threadCount = threadCount;
    this.maxQueueSize = maxQueueSize;
    this.notifyThreshold = notifyThreshold;
    this.keepAliveSeconds = keepAliveSeconds;
    this.threadFactory = threadFactory;
    initialize();
  }

  public synchronized void initialize() {
    if (needsInitialization) {
      queue = new ThresholdBlockingQueue<>(maxQueueSize, notifyThreshold);
      executor = new ThreadPoolExecutor(threadCount, threadCount, keepAliveSeconds, SECONDS, queue, threadFactory);
      executor.allowCoreThreadTimeOut(keepAliveSeconds > 0);
      needsInitialization = false;
    }
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

  public synchronized void shutdown() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(awaitTerminationSeconds, SECONDS)) {
        logger.warn("Timed out while waiting for executor to terminate");
      }
    } catch (@SuppressWarnings("unused") InterruptedException ex) {
      logger.warn("Interrupted while waiting for executor to terminate");
      currentThread().interrupt();
    } finally {
      needsInitialization = true;
    }
  }
}
