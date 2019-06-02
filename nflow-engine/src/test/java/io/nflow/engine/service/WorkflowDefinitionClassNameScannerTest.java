package io.nflow.engine.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.core.io.ClassPathResource;

import io.nflow.engine.internal.executor.BaseNflowTest;

public class WorkflowDefinitionClassNameScannerTest extends BaseNflowTest {

  @Mock
  private ClassPathResource nonSpringWorkflowListing;
  @Mock
  private WorkflowDefinitionService workflowDefinitionService;

  @Test
  public void definitionIsAdded() throws Exception {
    String dummyTestClassname = DummyTestWorkflow.class.getName();
    ByteArrayInputStream bis = new ByteArrayInputStream(dummyTestClassname.getBytes(UTF_8));
    when(nonSpringWorkflowListing.getInputStream()).thenReturn(bis);

    new WorkflowDefinitionClassNameScanner(workflowDefinitionService, nonSpringWorkflowListing);

    verify(workflowDefinitionService).addWorkflowDefinition(any(DummyTestWorkflow.class));
  }

  @Test
  public void listingResourceIsOptional() throws Exception {
    new WorkflowDefinitionClassNameScanner(workflowDefinitionService, null);

    verifyZeroInteractions(workflowDefinitionService);
  }

}
