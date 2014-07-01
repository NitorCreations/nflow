package com.nitorcreations.nflow.engine.internal.executor;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.nitorcreations.nflow.engine.internal.executor.CongestionControl;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;

@RunWith(MockitoJUnitRunner.class)
public class CongestionControlTest {
  CongestionControl congestionCtrl;
  Task task = new Task();
  AtomicInteger queueSize = new AtomicInteger(1);
  int threshold = 1;

  @Mock
  Environment env;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  ThreadPoolTaskExecutor pool;

  @Mock
  BlockingQueue<Runnable> queue;

  @Before
  public void setup() {
    when(env.getProperty("nflow.dispatcher.executor.queue.wait_until_threshold", Integer.class, 0))
        .thenReturn(threshold);
    when(pool.getThreadPoolExecutor().getQueue()).thenReturn(queue);
    when(queue.size()).thenAnswer(queueSizeAnswer());

    congestionCtrl = new CongestionControl(pool, env);
  }

  @Test
  public void waitDoesNotBlockWhenNotAboveQueueTheshold() throws Throwable {
    @SuppressWarnings("unused")
    class WaitDoesNotBlockWhenNotAboveQueueTheshold extends MultithreadedTestCase {
      @Override
      public void initialize() {
        queueSize.set(threshold);
      }

      public void thread1() throws InterruptedException {
        congestionCtrl.register(task);
        congestionCtrl.waitUntilQueueThreshold();
      }
    }
    TestFramework.runOnce(new WaitDoesNotBlockWhenNotAboveQueueTheshold());
  }

  @Test
  public void waitBlocksWhenAboveQueueThreshold() throws Throwable {
    @SuppressWarnings("unused")
    class WaitBlocksWhenAboveQueueThreshold extends MultithreadedTestCase {
      @Override
      public void initialize() {
        queueSize.set(threshold+1);
      }

      public void thread1() {
        congestionCtrl.register(task);
        try {
          congestionCtrl.waitUntilQueueThreshold();
          Assert.fail("should block");
        } catch (InterruptedException expected) {
        }
      }

      public void thread2() {
        waitForTick(1);
        getThread(1).interrupt();
      }
    }
    TestFramework.runOnce(new WaitBlocksWhenAboveQueueThreshold());
  }

  @Test
  public void completingTaskCanUnblockWaiter() throws Throwable {
    @SuppressWarnings("unused")
    class CompletingTaskCanUnblockWaiter extends MultithreadedTestCase {
      @Override
      public void initialize() {
        queueSize.set(threshold+1);
      }

      public void thread1() throws InterruptedException {
        congestionCtrl.register(task);
        congestionCtrl.waitUntilQueueThreshold();
      }

      public void thread2() {
        waitForTick(1);
        assertThat(queueSize.decrementAndGet(), is(1));
        task.callback.onSuccess(null);
      }
    }
    TestFramework.runOnce(new CompletingTaskCanUnblockWaiter());
  }

  @Test
  public void failingTaskCanUnblockWaiter() throws Throwable {
    @SuppressWarnings("unused")
    class FailingTaskCanUnblockWaiter extends MultithreadedTestCase {
      @Override
      public void initialize() {
        queueSize.set(threshold+1);
      }

      public void thread1() throws InterruptedException {
        congestionCtrl.register(task);
        congestionCtrl.waitUntilQueueThreshold();
      }

      public void thread2() {
        waitForTick(1);
        assertThat(queueSize.decrementAndGet(), is(threshold));
        task.callback.onFailure(null);
      }
    }
    TestFramework.runOnce(new FailingTaskCanUnblockWaiter());
  }

  private Answer<Integer> queueSizeAnswer() {
    return new Answer<Integer>() {
      @Override
      public Integer answer(InvocationOnMock invocation) {
        return queueSize.get();
      }
    };
  }

  static class Task implements ListenableFuture<Object> {
    ListenableFutureCallback<? super Object> callback;

    @Override
    public void addCallback(ListenableFutureCallback<? super Object> newCallback) {
      this.callback = newCallback;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return false;
    }

    @Override
    public Object get() {
      return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) {
      return null;
    }
  }
}