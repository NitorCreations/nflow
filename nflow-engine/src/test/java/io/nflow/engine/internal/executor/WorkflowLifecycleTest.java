package io.nflow.engine.internal.executor;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import io.nflow.engine.service.WorkflowDefinitionService;

@ExtendWith(MockitoExtension.class)
public class WorkflowLifecycleTest {

  @Mock
  private WorkflowDispatcher dispatcher;
  @Mock
  private ThreadFactory threadFactory;
  @Mock
  private Environment env;
  @Mock
  private Thread dispatcherThread;
  @Mock
  private WorkflowDefinitionService workflowDefinitions;

  private WorkflowLifecycle lifecycle;

  @BeforeEach
  public void setup() {
    when(env.getRequiredProperty("nflow.autoinit", Boolean.class)).thenReturn(TRUE);
    when(env.getRequiredProperty("nflow.autostart", Boolean.class)).thenReturn(TRUE);
    when(threadFactory.newThread(dispatcher)).thenReturn(dispatcherThread);
    lifecycle = new WorkflowLifecycle(workflowDefinitions, dispatcher, threadFactory, env);
  }

  @Test
  public void getPhaseIsMaximum() {
    assertThat(lifecycle.getPhase(), is(Integer.MAX_VALUE));
  }

  @Test
  public void isAutostartSet() {
    assertThat(lifecycle.isAutoStartup(), is(true));
  }

  @Test
  public void startLaunchesDispatcherThread() {
    lifecycle.start();
    verify(dispatcherThread).start();
  }

  @Test
  public void stopStopsDispatcherThread() {
    lifecycle.stop();
    verify(dispatcher).shutdown();
  }

  @Test
  public void stopWithCallbackStopsDispatcherThreadAndRunsCallback() {
    Runnable callback = mock(Runnable.class);
    lifecycle.stop(callback);
    verify(dispatcher).shutdown();
    verify(callback).run();
  }

  @Test
  public void pauseAndResumeWorks() {
    lifecycle.pause();
    verify(dispatcher).pause();
    lifecycle.resume();
    verify(dispatcher).resume();
    lifecycle.isPaused();
    verify(dispatcher).isPaused();
  }

//  @Test
//  public void isRunningReturnsDispatcherThreadStatus() {
//    when(dispatcherThread.isAlive()).thenReturn(true); // native method mocking would require PowerMock
//    assertThat(lifecycle.isRunning(), is(true));
//    verify(dispatcherThread).isAlive();
//  }

}
