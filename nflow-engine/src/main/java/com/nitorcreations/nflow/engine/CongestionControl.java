package com.nitorcreations.nflow.engine;

import java.util.concurrent.BlockingQueue;

import javax.inject.Inject;

import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
public class CongestionControl {
  private final BlockingQueue<Runnable> queue;
  private final int waitUntilQueueThreshold;
  private final Monitor monitor = new Monitor();

  @Inject
  public CongestionControl(ThreadPoolTaskExecutor pool, Environment env) {
    this.waitUntilQueueThreshold =
        env.getProperty("nflow.dispatcher.executor.queue.wait_until_threshold", Integer.class, 0);
    this.queue = pool.getThreadPoolExecutor().getQueue();
  }

  public synchronized void waitUntilQueueUnderThreshold() throws InterruptedException {
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
