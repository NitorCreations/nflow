package com.nitorcreations.nflow.engine.internal.executor;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.nitorcreations.nflow.engine.internal.dao.ExecutorDao;
import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowDispatcherTest {
  WorkflowDispatcher dispatcher;
  WorkflowInstanceExecutor executor;

  @Mock WorkflowInstanceDao workflowInstances;
  @Mock ExecutorDao recovery;
  @Mock WorkflowStateProcessorFactory executorFactory;

  MockEnvironment env = new MockEnvironment();

  @Before
  public void setup() {
    env.setProperty("nflow.dispatcher.sleep.ms", "0");
    env.setProperty("nflow.dispatcher.executor.queue.wait_until_threshold", "0");
    env.setProperty("nflow.illegal.state.change.action", "ignore");
    env.setProperty("nflow.unknown.workflow.type.retry.delay.minutes", "60");
    env.setProperty("nflow.unknown.workflow.state.retry.delay.minutes", "60");
    when(recovery.isTransactionSupportEnabled()).thenReturn(true);
    executor = new WorkflowInstanceExecutor(3, 2, 0, 10, 0, new CustomizableThreadFactory("nflow-executor-"));
    dispatcher = new WorkflowDispatcher(executor, workflowInstances, executorFactory, recovery, env);
  }

  @Test(expected = BeanCreationException.class)
  public void workflowDispatcherCreationFailsWithoutTransactionSupport() {
    when(recovery.isTransactionSupportEnabled()).thenReturn(false);
    new WorkflowDispatcher(executor, workflowInstances, executorFactory, recovery, env);
  }

  @Test
  public void exceptionDuringDispatcherExecutionCausesRetry() throws Throwable {
    @SuppressWarnings("unused")
    class ExceptionDuringDispatcherExecutionCausesRetry extends MultithreadedTestCase {
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt()))
            .thenReturn(ids(1))
            .thenThrow(new RuntimeException("Expected: exception during dispatcher execution"))
            .thenAnswer(waitForTickAndAnswer(2, ids(2), this));
        WorkflowStateProcessor fakeWorkflowExecutor = fakeWorkflowExecutor(1, noOpRunnable());
        when(executorFactory.createProcessor(1)).thenReturn(fakeWorkflowExecutor);
        WorkflowStateProcessor fakeWorkflowExecutor2 = fakeWorkflowExecutor(2, noOpRunnable());
        when(executorFactory.createProcessor(2)).thenReturn(fakeWorkflowExecutor2);

        dispatcher.run();
      }

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
      }

      @Override
      public void finish() {
        verify(workflowInstances, times(3)).pollNextWorkflowInstanceIds(anyInt());
        InOrder inOrder = inOrder(executorFactory);
        inOrder.verify(executorFactory).createProcessor(1);
        inOrder.verify(executorFactory).createProcessor(2);
      }
    }
    TestFramework.runOnce(new ExceptionDuringDispatcherExecutionCausesRetry());
  }

  @Test
  public void errorDuringDispatcherExecutionStopsDispatcher() throws Throwable {
    @SuppressWarnings("unused")
    class ErrorDuringDispatcherExecutionStopsDispatcher extends MultithreadedTestCase {
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt()))
            .thenThrow(new AssertionError())
            .thenReturn(ids(1));

        try {
          dispatcher.run();
          Assert.fail("Error should stop the dispatcher");
        } catch (AssertionError expected) {
          assertPoolIsShutdown(true);
        }
      }

      @Override
      public void finish() {
        verify(workflowInstances).pollNextWorkflowInstanceIds(anyInt());
        verify(executorFactory, never()).createProcessor(anyInt());
      }
    }
    TestFramework.runOnce(new ErrorDuringDispatcherExecutionStopsDispatcher());
  }

  @Test
  public void emptyPollResultCausesNoTasksToBeScheduled() throws Throwable {
    @SuppressWarnings("unused")
    class EmptyPollResultCausesNoTasksToBeScheduled extends MultithreadedTestCase {
      @SuppressWarnings("unchecked")
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt()))
            .thenReturn(ids(), ids())
            .thenAnswer(waitForTickAndAnswer(2, ids(), this));
        dispatcher.run();
      }

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
      }

      @Override
      public void finish() {
        verify(workflowInstances, times(3)).pollNextWorkflowInstanceIds(anyInt());
        verify(executorFactory, never()).createProcessor(anyInt());
      }
    }
    TestFramework.runOnce(new EmptyPollResultCausesNoTasksToBeScheduled());
  }

  @Test
  public void shutdownBlocksUntilPoolShutdown() throws Throwable {
    @SuppressWarnings("unused")
    class ShutdownBlocksUntilPoolShutdown extends MultithreadedTestCase {
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt()))
            .thenAnswer(waitForTickAndAnswer(2, ids(1), this));
        WorkflowStateProcessor fakeWorkflowExecutor = fakeWorkflowExecutor(1, waitForTickRunnable(3, this));
        when(executorFactory.createProcessor(anyInt())).thenReturn(fakeWorkflowExecutor);

        dispatcher.run();
      }

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
        assertPoolIsShutdown(true);
      }
    }
    TestFramework.runOnce(new ShutdownBlocksUntilPoolShutdown());
  }

  @Test
  public void shutdownCanBeInterrupted() throws Throwable {
    @SuppressWarnings("unused")
    class ShutdownCanBeInterrupted extends MultithreadedTestCase {
      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            waitForTick(2);
            getThreadByName("threadShutdown").interrupt();
            return ids(1);
          }
        });
        WorkflowStateProcessor fakeWorkflowExecutor = fakeWorkflowExecutor(1, waitForTickRunnable(3, this));
        when(executorFactory.createProcessor(anyInt())).thenReturn(fakeWorkflowExecutor);

        dispatcher.run();
      }

      public void threadShutdown() {
        assertPoolIsShutdown(false);
        waitForTick(1);
        dispatcher.shutdown();
        waitForTick(3);
        assertPoolIsShutdown(true);
      }
    }
    TestFramework.runOnce(new ShutdownCanBeInterrupted());
  }

  @Test
  public void exceptionOnPoolShutdownIsNotPropagated() throws Throwable {
    @SuppressWarnings("unused")
    class ExceptionOnPoolShutdownIsNotPropagated extends MultithreadedTestCase {
      private WorkflowInstanceExecutor poolSpy;

      @Override
      public void initialize() {
        poolSpy = Mockito.spy(executor);
        dispatcher = new WorkflowDispatcher(poolSpy, workflowInstances, executorFactory, recovery, env);
      }

      public void threadDispatcher() {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(waitForTickAndAnswer(2, ids(), this));
        doThrow(new RuntimeException("Expected: exception on pool shutdown")).when(poolSpy).shutdown();

        dispatcher.run();
      }

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
      }

      @Override
      public void finish() {
        verify(poolSpy).shutdown();
      }
    }
    TestFramework.runOnce(new ExceptionOnPoolShutdownIsNotPropagated());
  }

  @Test
  public void shutdownCanBeCalledMultipleTimes() throws Throwable {
    @SuppressWarnings("unused")
    class ShutdownCanBeCalledMultipleTimes extends MultithreadedTestCase {
      public void threadDispatcher() throws InterruptedException {
        when(workflowInstances.pollNextWorkflowInstanceIds(anyInt())).thenAnswer(waitForTickAndAnswer(2, ids(), this));
        dispatcher.run();
      }

      public void threadShutdown() {
        waitForTick(1);
        dispatcher.shutdown();
      }

      @Override
      public void finish() {
        assertPoolIsShutdown(true);
        dispatcher.shutdown();
      }
    }
    TestFramework.runOnce(new ShutdownCanBeCalledMultipleTimes());
  }

  void assertPoolIsShutdown(boolean isTrue) {
    assertEquals(isTrue, executor.executor.isShutdown());
  }

  Runnable noOpRunnable() {
    return new Runnable() {
      @Override
      public void run() {
      }
    };
  }

  Runnable waitForTickRunnable(final int tick, final MultithreadedTestCase mtc) {
    return new Runnable() {
      @Override
      public void run() {
        mtc.waitForTick(tick);
      }
    };
  }

  WorkflowStateProcessor fakeWorkflowExecutor(int instanceId, final Runnable fakeCommand) {
    return new WorkflowStateProcessor(instanceId, null, null, null, null, null, env, (WorkflowExecutorListener) null) {
      @Override
      public void run() {
        fakeCommand.run();
      }
    };
  }

  Answer<List<Integer>> waitForTickAndAnswer(final int tick, final List<Integer> answer, final MultithreadedTestCase mtc) {
    return new Answer<List<Integer>>() {
      @Override
      public List<Integer> answer(InvocationOnMock invocation) {
        mtc.waitForTick(tick);
        return answer;
      }
    };
  }

  @SuppressWarnings("serial")
  static class ThreadPoolTaskExecutorWithThresholdQueue extends ThreadPoolTaskExecutor {
    @Override
    protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
      return new ThresholdBlockingQueue<>(queueCapacity, 0);
    }
  }

  static List<Integer> ids(Integer... ids) {
    return asList(ids);
  }
}
