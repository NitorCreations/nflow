package io.nflow.engine.internal.executor;

import static java.lang.Math.min;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.joda.time.DateTime;
import org.slf4j.Logger;

public class WorkflowInstanceExecutor {
  private static final Logger logger = getLogger(WorkflowInstanceExecutor.class);

  private final int awaitTerminationSeconds;
  private final int threadCount;
  final ThreadPoolExecutor executor;
  final ThresholdBlockingQueue<Runnable> queue;

  public WorkflowInstanceExecutor(int maxQueueSize, int threadCount, int notifyThreshold, int awaitTerminationSeconds,
      int keepAliveSeconds, ThreadFactory threadFactory) {
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

  public boolean shutdown(Consumer<List<Long>> clearExecutorIds) {
    // Hard timeout is 1/3 of configured total timeout, but never more than 5 seconds
    var totalTimeoutMs = SECONDS.toMillis(awaitTerminationSeconds);
    var hardTimeoutMs = min(5000, totalTimeoutMs / 3);
    var gracefulTimeoutMs = totalTimeoutMs - hardTimeoutMs;
    // step 1: stop accepting new jobs
    executor.shutdown();
    // step 2: drain the not started workflows from the queue and mark their executorId to null which makes them immediately
    // runnable by other executors
    List<Runnable> queuedWorkflows = new ArrayList<>();
    queue.drainTo(queuedWorkflows);
    boolean executorIdsCleared = clearExecutorIds(queuedWorkflows, clearExecutorIds);
    try {
      // step 3: wait for executing workflow processing to complete normally
      if (!executor.awaitTermination(gracefulTimeoutMs, MILLISECONDS)) {
        logger.warn("Graceful shutdown timed out after {} ms while waiting for workflow processing to complete normally",
            gracefulTimeoutMs);
        // step 4: interrupt workflows that are still executing
        executor.shutdownNow();
        // step 5: wait for interrupted workflow processing to complete
        if (!executor.awaitTermination(hardTimeoutMs, MILLISECONDS)) {
          logger.warn("Hard shutdown timed out after {} ms while waiting for interrupted workflow processing to complete",
              hardTimeoutMs);
        }
      }
    } catch (@SuppressWarnings("unused") InterruptedException ex) {
      logger.warn("Interrupted while waiting for executor to terminate");
      currentThread().interrupt();
    }
    // step 6: check if the executor threads were successfully terminated and executorIds of not started workflows were cleared
    var gracefulShutdownSucceeded = executorIdsCleared && executor.isTerminated();
    if (gracefulShutdownSucceeded) {
      logger.info("Graceful shutdown succeeded");
    }
    return gracefulShutdownSucceeded;
  }

  @SuppressFBWarnings(value = "EXS_EXCEPTION_SOFTENING_RETURN_FALSE", justification = "Shutdown error handling only needs the boolean")
  private boolean clearExecutorIds(List<Runnable> workflows, Consumer<List<Long>> clearExecutorIds) {
    if (workflows.isEmpty()) {
      return true;
    }
    try {
      @SuppressWarnings("unchecked")
      var wfs = (List<WorkflowStateProcessor>) (Object) workflows;
      clearExecutorIds.accept(wfs.stream().map(w -> w.instanceId).collect(toList()));
      return true;
    } catch (Exception e) {
      logger.error("Failed to clear executorIds of queued workflows", e);
      return false;
    }
  }
}
