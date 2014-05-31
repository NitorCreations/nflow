package com.nitorcreations.nflow.engine;

import java.util.concurrent.BlockingQueue;

import javax.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
public class CongestionControl {
  private final Monitor monitor;

  @Inject
  public CongestionControl(ThreadPoolTaskExecutor pool, Environment env) {
    monitor = new Monitor(pool.getThreadPoolExecutor().getQueue(), env.getProperty("nflow.dispatcher.executor.queue.wait_until_threshold", Integer.class, 0));
  }

  public void waitUntilQueueThreshold() throws InterruptedException {
    monitor.waitUntilQueueThreshold();
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

    synchronized void waitUntilQueueThreshold() throws InterruptedException {
      while (queue.size() > waitUntilQueueThreshold) {
        wait();
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
