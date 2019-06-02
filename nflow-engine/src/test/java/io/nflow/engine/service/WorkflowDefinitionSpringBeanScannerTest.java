package io.nflow.engine.service;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import io.nflow.engine.internal.executor.BaseNflowTest;

public class WorkflowDefinitionSpringBeanScannerTest extends BaseNflowTest {

  @Mock
  private WorkflowDefinitionService workflowDefinitionService;
  @Mock
  private DummyTestWorkflow definition;

  @Test
  public void definitionIsAdded() {
    new WorkflowDefinitionSpringBeanScanner(workflowDefinitionService, asList(definition));

    verify(workflowDefinitionService).addWorkflowDefinition(definition);
  }

}
