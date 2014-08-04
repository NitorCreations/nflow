package com.nitorcreations.nflow.engine.internal.executor;

import static org.joda.time.DateTimeUtils.currentTimeMillis;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

public class ThresholdBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
  private final LinkedBlockingQueue<E> queue;
  private final int notifyThreshHold;

  public ThresholdBlockingQueue(int capacity, int notifyThreshHold) {
    this.notifyThreshHold = notifyThreshHold;
    queue = new LinkedBlockingQueue<>(capacity);
  }

  private void notifyIf() {
    int size = queue.size();
    synchronized (this) {
      if (size <= notifyThreshHold) {
        notifyAll();
      }
    }
  }

  public synchronized void waitUntilQueueSizeLowerThanThreshold(DateTime waitUntil) throws InterruptedException {
    while (queue.size() > notifyThreshHold) {
      long sleep = waitUntil.getMillis() - currentTimeMillis();
      if (sleep <= 0) {
        break;
      }
      wait(sleep);
    }
  }

  @Override
  public boolean offer(E e) {
    return queue.offer(e);
  }

  @Override
  public E poll() {
    E o = queue.poll();
    notifyIf();
    return o;
  }

  @Override
  public E peek() {
    return queue.peek();
  }

  @Override
  public Iterator<E> iterator() {
    return queue.iterator();
  }

  @Override
  public int size() {
    return queue.size();
  }

  @Override
  public void put(E e) throws InterruptedException {
    queue.put(e);
  }

  @Override
  public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
    return queue.offer(e, timeout, unit);
  }

  @Override
  public E take() throws InterruptedException {
    E o = queue.take();
    notifyIf();
    return o;
  }

  @Override
  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    E o = queue.poll(timeout, unit);
    notifyIf();
    return o;
  }

  @Override
  public int remainingCapacity() {
    return queue.remainingCapacity();
  }

  @Override
  public int drainTo(Collection<? super E> c) {
    int count = queue.drainTo(c);
    notifyIf();
    return count;
  }

  @Override
  public int drainTo(Collection<? super E> c, int maxElements) {
    int count = queue.drainTo(c, maxElements);
    notifyIf();
    return count;
  }
}
