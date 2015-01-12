package com.nitorcreations.nflow.engine.internal.executor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowInstanceExecutorTest {

  @Mock
  ThreadFactory threadFactory;
  @Mock
  Runnable runnable;

  @Test
  public void testThreadPoolCreateWithCorrectParameters() {
    WorkflowInstanceExecutor t = new WorkflowInstanceExecutor(3, 2, 1, 3, 4, threadFactory);
    assertThat(t.executor.getCorePoolSize(), is(2));
    assertThat(t.executor.getMaximumPoolSize(), is(2));
    assertThat(t.executor.getKeepAliveTime(SECONDS), is(4L));
    assertThat(t.executor.allowsCoreThreadTimeOut(), is(true));
    assertThat(t.executor.getThreadFactory(), sameInstance(threadFactory));
    assertThat(t.executor.getQueue(), sameInstance((BlockingQueue<Runnable>) t.queue));
  }

  @Test
  public void testDummyGetters() {
    WorkflowInstanceExecutor t = new WorkflowInstanceExecutor(3, 2, 1, 3, 4, threadFactory);
    assertThat(t.getQueueRemainingCapacity(), is(3));
  }

  @Test
  public void testExecute() {
    WorkflowInstanceExecutor t = new WorkflowInstanceExecutor(3, 2, 1, 3, 4, new CustomizableThreadFactory("test"));
    t.execute(runnable);
    verify(runnable, timeout(1000)).run();
  }

  @Test
  public void testWait() throws InterruptedException {
    WorkflowInstanceExecutor t = new WorkflowInstanceExecutor(3, 2, 1, 3, 4, new CustomizableThreadFactory("test"));
    t.execute(runnable);
    t.waitUntilQueueSizeLowerThanThreshold(new DateTime().plusSeconds(5));
  }

  @Test
  public void testShutdown() {
    WorkflowInstanceExecutor t = new WorkflowInstanceExecutor(3, 2, 1, 3, 4, new CustomizableThreadFactory("test"));
    t.shutdown();
    assertThat(t.executor.isShutdown(), is(true));
  }
}
