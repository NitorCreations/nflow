package com.nitorcreations.nflow.engine.internal.executor;

import static org.joda.time.DateTimeUtils.currentTimeMillis;

import java.util.concurrent.BlockingQueue;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class CongestionControl {
  private final Monitor monitor;

  @Inject
  public CongestionControl(ThreadPoolTaskExecutor pool, Environment env) {
    monitor = new Monitor(pool.getThreadPoolExecutor().getQueue(), env.getProperty("nflow.dispatcher.executor.queue.wait_until_threshold", Integer.class, 0));
  }

  public void waitUntilQueueThreshold(DateTime waitUntil) throws InterruptedException {
    monitor.waitUntilQueueThreshold(waitUntil);
  }

  public void register(ListenableFuture<?> listenableFuture) {
    listenableFuture.addCallback(monitor);
  }

  @SuppressFBWarnings(value="NN_NAKED_NOTIFY", justification = "callback methods are called after mutable state change")
  private static class Monitor implements ListenableFutureCallback<Object> {
    private final BlockingQueue<Runnable> queue;
    private final int waitUntilQueueThreshold;

    Monitor(BlockingQueue<Runnable> queue, int waitUntilQueueThreshold) {
      this.queue = queue;
      this.waitUntilQueueThreshold = waitUntilQueueThreshold;
    }

    synchronized void waitUntilQueueThreshold(DateTime waitUntil) throws InterruptedException {
      while (queue.size() > waitUntilQueueThreshold) {
        long sleep = waitUntil.getMillis() - currentTimeMillis();
        if (sleep <= 0) {
          break;
        }
        wait(sleep);
      }
    }

    @Override
    public synchronized void onSuccess(Object result) {
      notifyAll();
    }

    @Override
    public synchronized void onFailure(Throwable t) {
      notifyAll();
    }
  }
}
