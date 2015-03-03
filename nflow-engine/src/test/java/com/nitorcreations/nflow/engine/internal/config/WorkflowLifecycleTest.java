package com.nitorcreations.nflow.engine.internal.config;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ThreadFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

import com.nitorcreations.nflow.engine.internal.executor.WorkflowDispatcher;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowLifecycleTest {

  @Mock
  private WorkflowDispatcher dispatcher;
  @Mock
  private ThreadFactory threadFactory;
  @Mock
  private Environment env;
  @Mock
  private Thread dispatcherThread;

  private WorkflowLifecycle lifecycle;

  @Before
  public void setup() {
    when(env.getRequiredProperty("nflow.autostart", Boolean.class)).thenReturn(TRUE);
    when(threadFactory.newThread(dispatcher)).thenReturn(dispatcherThread);
    lifecycle = new WorkflowLifecycle(dispatcher, threadFactory, env);
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

//  @Test
//  public void isRunningReturnsDispatcherThreadStatus() {
//    when(dispatcherThread.isAlive()).thenReturn(true); // native method mocking would require PowerMock
//    assertThat(lifecycle.isRunning(), is(true));
//    verify(dispatcherThread).isAlive();
//  }

}
