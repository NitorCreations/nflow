package com.nitorcreations.nflow.engine;

import java.util.concurrent.BlockingQueue;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

public class CongestionControl {
  private final BlockingQueue<Runnable> queue;
  private final int waitUntilQueueThreshold;
  private final Monitor monitor = new Monitor();

  public CongestionControl(ThreadPoolTaskExecutor pool, int waitUntilQueueThreshold) {
    this.waitUntilQueueThreshold = waitUntilQueueThreshold;
    this.queue = pool.getThreadPoolExecutor().getQueue();
  }

  synchronized void waitUntilQueueUnderThreshold() throws InterruptedException {
    monitor.waitUntilQueueUnderThreshold();
  }

  public void register(ListenableFuture<?> listenableFuture) {
    listenableFuture.addCallback(monitor);
  }

  private class Monitor implements ListenableFutureCallback<Object> {
    public synchronized void waitUntilQueueUnderThreshold() throws InterruptedException {
      while (queue.size() > waitUntilQueueThreshold) {
        monitor.wait();
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
