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
  private WorkflowDefinitionSpringBeanScanner scanner;

  @Test
  public void definitionIsAdded() {
    scanner = new WorkflowDefinitionSpringBeanScanner(workflowDefinitionService);

    scanner.setWorkflowDefinitions(asList(definition));

    verify(workflowDefinitionService).addWorkflowDefinition(definition);
  }

}
