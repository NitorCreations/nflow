package com.nitorcreations.nflow.engine.service;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.internal.dao.ExecutorDao;
import com.nitorcreations.nflow.engine.internal.executor.BaseNflowTest;
import com.nitorcreations.nflow.engine.workflow.executor.WorkflowExecutor;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowExecutorServiceTest extends BaseNflowTest {

  private WorkflowExecutorService service;

  @Mock
  private ExecutorDao executorDao;
  @Mock
  private WorkflowExecutor executor;

  @Before
  public void setup() {
    when(executorDao.getExecutors()).thenReturn(asList(executor));
    service = new WorkflowExecutorService(executorDao);
  }

  @Test
  public void getWorkflowExecutorsWorks() {
    assertThat(service.getWorkflowExecutors().size(), is(equalTo(1)));
  }
}
