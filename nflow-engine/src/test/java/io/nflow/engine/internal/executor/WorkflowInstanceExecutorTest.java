package io.nflow.engine.internal.executor;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

@ExtendWith(MockitoExtension.class)
public class WorkflowInstanceExecutorTest {

  @Mock
  ThreadFactory threadFactory;
  @Mock
  WorkflowStateProcessor runnable;

  @Test
  public void testThreadPoolCreateWithCorrectParameters() {
    WorkflowInstanceExecutor t = new WorkflowInstanceExecutor(3, 2, 1, 3, 4, threadFactory);
    assertThat(t.executor.getCorePoolSize(), is(2));
    assertThat(t.executor.getMaximumPoolSize(), is(2));
    assertThat(t.executor.getKeepAliveTime(SECONDS), is(4L));
    assertThat(t.executor.allowsCoreThreadTimeOut(), is(true));
    assertThat(t.executor.getThreadFactory(), sameInstance(threadFactory));
    assertThat(t.executor.getQueue(), sameInstance(t.queue));
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
    assertThat(t.shutdown(workflows -> assertThat(workflows, empty())), is(true));
    assertThat(t.executor.isShutdown(), is(true));
  }


  private final AtomicReference<Boolean> wasInterrupted = new AtomicReference<>();
  @Test
  public void testShutdownWithQueuedEntries() throws Exception {
    WorkflowInstanceExecutor t = new WorkflowInstanceExecutor(3, 1, 1, 3, 4, new CustomizableThreadFactory("test"));
    var slowMock = mock(WorkflowStateProcessor.class, "SlowMock");
    doAnswer(a -> silentSleep(5000) ).when(slowMock).run();
    // 1 thread stuck running
    t.execute(slowMock);
    // another goes to queue
    t.execute(runnable);
    assertThat(t.shutdown(workflows -> {
      assertThat(workflows, is(List.of(runnable)));
      workflows.clear();
    }), is(true));
    assertThat(t.executor.isShutdown(), is(true));
    assertThat(wasInterrupted.get(), is(TRUE));
  }

  private Object silentSleep(int millis) {
    boolean interrupted = false;
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      interrupted = true;
    }
    wasInterrupted.set(interrupted);
    return null;
  }
}
