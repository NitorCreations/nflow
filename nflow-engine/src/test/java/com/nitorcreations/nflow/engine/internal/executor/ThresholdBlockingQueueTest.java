package com.nitorcreations.nflow.engine.internal.executor;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.joda.time.DateTime;
import org.junit.Test;

public class ThresholdBlockingQueueTest {
  ThresholdBlockingQueue<Integer> q = new ThresholdBlockingQueue<>(3, 1);

  @Test
  public void worksAsQueue() throws InterruptedException {
    assertThat(q.isEmpty(), is(true));
    assertThat(q.offer(1), is(true));
    q.put(2);
    assertThat(q.offer(3), is(true));
    assertThat(q.size(), is(3));
    assertThat(q.remainingCapacity(), is(0));
    assertThat(q.offer(4), is(false));
    assertThat(q.offer(4, 5, MILLISECONDS), is(false));
    assertThat(q.peek(), is(1));
    assertThat(q.take(), is(1));
    assertThat(q.poll(), is(2));
    assertThat(q.poll(5, MILLISECONDS), is(3));

    ArrayList<Integer> l = new ArrayList<>();
    q.put(1);
    assertThat(q.iterator().next(), is(1));
    q.drainTo(l);
    assertThat(l, is(asList(1)));

    l.clear();
    q.put(2);
    q.put(3);
    q.drainTo(l, 1);
    assertThat(l, is(asList(2)));
  }

  @Test(timeout = 5000)
  public void doesNotWaitIfQueueIsAlreadyBelowThreshold() throws InterruptedException {
    q.waitUntilQueueSizeLowerThanThreshold(new DateTime().plusMinutes(1));
  }

  @Test(timeout = 10000)
  public void waitsUntilQueueSizeLowerThanThreshold() throws InterruptedException, ExecutionException {
    q.put(100);
    q.put(200);
    q.put(300);
    Callable<Integer> tester = new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        q.waitUntilQueueSizeLowerThanThreshold(new DateTime().plusMinutes(1));
        return q.size();
      }
    };
    Future<Integer> result = newSingleThreadExecutor().submit(tester);
    sleep(1000);
    assertThat(result.isDone(), is(false)); // step 1, q.size = 3, thread is blocking

    q.poll();
    sleep(1000);
    assertThat(result.isDone(), is(false)); // step 2, q.size = 2, thread is still blocking

    q.poll();
    sleep(1000);
    assertThat(result.isDone(), is(true)); // step 3, q.size = 1, thread wakes up

    assertThat(result.get(), is(1));
  }

  @Test(timeout = 10000)
  public void waitTimeoutWorks() throws InterruptedException, ExecutionException {
    q.put(100);
    q.put(200);
    q.put(300);
    Callable<Integer> tester = new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        q.waitUntilQueueSizeLowerThanThreshold(new DateTime().plusSeconds(3));
        return q.size();
      }
    };
    Future<Integer> result = newSingleThreadExecutor().submit(tester);
    sleep(1000);
    // send extra notify
    synchronized (q) {
      q.notifyAll();
    }
    sleep(1000);
    assertThat(result.isDone(), is(false)); // despite the notify the thread is still blocked

    sleep(2000);

    assertThat(result.isDone(), is(true)); // after 4 seconds the thread returns

    assertThat(result.get(), is(3));
  }
}
