package io.nflow.engine.workflow.curated;

import static io.nflow.engine.workflow.curated.CronWorkflow.SCHEDULE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.engine.service.ExecutorMaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;

@ExtendWith(MockitoExtension.class)
public class ExecutorMaintenanceWorkflowTest {

  @Mock
  private MaintenanceService maintenanceService;
  @Mock
  private StateExecution execution;
  @Mock
  private ExecutorMaintenanceConfiguration config;
  @InjectMocks
  private final ExecutorMaintenanceWorkflow workflow = new ExecutorMaintenanceWorkflow();

  @Test
  void doWorkCallsMaintenanceService() {
    when(maintenanceService.cleanupExecutors(config)).thenReturn(10);

    NextAction nextAction = workflow.doWork(execution, config);

    assertThat(nextAction.getReason(), is("Deleted 10 executors"));
    assertThat(nextAction.getNextState(), is(SCHEDULE));
    verify(maintenanceService).cleanupExecutors(config);
  }
}
