package com.nitorcreations.nflow.engine;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;
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
  public void waitDoesNotBlockWhenUnderQueueTheshold() throws Throwable {
    class WaitDoesNotBlockWhenUnderQueueTheshold extends MultithreadedTestCase {
      @Override
      public void initialize() {
        queueSize.set(threshold);
      }

      public void thread1() throws InterruptedException {
        congestionCtrl.register(task);
        congestionCtrl.waitUntilQueueUnderThreshold();
      }
    }
    TestFramework.runOnce(new WaitDoesNotBlockWhenUnderQueueTheshold());
  }

  @Test
  public void waitsUntilQueueIsUnderThreshold() throws Throwable {
    class WaitsUntilQueueIsUnderThreshold extends MultithreadedTestCase {
      @Override
      public void initialize() {
        queueSize.set(threshold+1);
      }

      public void thread1() {
        congestionCtrl.register(task);
        try {
          congestionCtrl.waitUntilQueueUnderThreshold();
          fail("should block");
        } catch (InterruptedException expected) {
        }
      }

      public void thread2() {
        waitForTick(1);
        getThread(1).interrupt();
      }
    }
    TestFramework.runOnce(new WaitsUntilQueueIsUnderThreshold());
  }

  @Test
  public void completingTaskCanUnblockWaiter() throws Throwable {
    class CompletingTaskCanUnblockWaiter extends MultithreadedTestCase {
      @Override
      public void initialize() {
        queueSize.set(threshold+1);
      }

      public void thread1() throws InterruptedException {
        congestionCtrl.register(task);
        congestionCtrl.waitUntilQueueUnderThreshold();
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
    class FailingTaskCanUnblockWaiter extends MultithreadedTestCase {
      @Override
      public void initialize() {
        queueSize.set(threshold+1);
      }

      public void thread1() throws InterruptedException {
        congestionCtrl.register(task);
        congestionCtrl.waitUntilQueueUnderThreshold();
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
      public Integer answer(InvocationOnMock invocation) throws Throwable {
        return queueSize.get();
      }
    };
  }

  static class Task implements ListenableFuture<Object> {
    ListenableFutureCallback<? super Object> callback;

    @Override
    public void addCallback(ListenableFutureCallback<? super Object> callback) {
      this.callback = callback;
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
    public Object get() throws InterruptedException, ExecutionException {
      return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return null;
    }
  }
}