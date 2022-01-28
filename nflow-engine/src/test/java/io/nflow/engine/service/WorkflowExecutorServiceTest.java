package io.nflow.engine.service;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.engine.internal.dao.ExecutorDao;
import io.nflow.engine.internal.executor.BaseNflowTest;
import io.nflow.engine.workflow.executor.WorkflowExecutor;

@ExtendWith(MockitoExtension.class)
public class WorkflowExecutorServiceTest extends BaseNflowTest {

  private WorkflowExecutorService service;

  @Mock
  private ExecutorDao executorDao;
  @Mock
  private WorkflowExecutor executor;

  @BeforeEach
  public void setup() {
    when(executorDao.getExecutors()).thenReturn(asList(executor));
    service = new WorkflowExecutorService(executorDao);
  }

  @Test
  public void getWorkflowExecutorsWorks() {
    assertThat(service.getWorkflowExecutors().size(), is(equalTo(1)));
  }
}
