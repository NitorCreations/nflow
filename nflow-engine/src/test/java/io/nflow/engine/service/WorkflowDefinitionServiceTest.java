package io.nflow.engine.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.core.env.Environment;

import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.internal.executor.BaseNflowTest;

public class WorkflowDefinitionServiceTest extends BaseNflowTest {

  @Mock
  private WorkflowDefinitionDao workflowDefinitionDao;
  @Mock
  private Environment env;
  @Mock
  private DummyTestWorkflow workflowDefinition;
  private WorkflowDefinitionService service;

  @BeforeEach
  public void setup() {
    lenient().when(workflowDefinition.getType()).thenReturn("dummy");
  }

  private void initializeService(boolean definitionPersist, boolean autoInit) {
    when(env.getRequiredProperty("nflow.definition.persist", Boolean.class)).thenReturn(definitionPersist);
    when(env.getRequiredProperty("nflow.autoinit", Boolean.class)).thenReturn(autoInit);
    service = new WorkflowDefinitionService(workflowDefinitionDao, env);
  }

  @Test
  public void addedDefinitionIsStoredWhenAutoInitIsTrue() {
    initializeService(true, true);

    service.addWorkflowDefinition(workflowDefinition);

    verify(workflowDefinitionDao).storeWorkflowDefinition(workflowDefinition);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void addedDefinitionIsNotStoredWhenAutoInitIsFalse() {
    initializeService(true, false);

    service.addWorkflowDefinition(workflowDefinition);

    verifyNoInteractions(workflowDefinitionDao);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void addedDefinitionIsNotStoredWhenDefinitionPersistIsFalse() {
    initializeService(false, true);

    service.addWorkflowDefinition(workflowDefinition);

    verifyNoInteractions(workflowDefinitionDao);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void definitionsAreStoredDuringPostProcessingWhenAutoInitIsFalse() {
    initializeService(true, false);
    service.addWorkflowDefinition(workflowDefinition);

    service.postProcessWorkflowDefinitions();

    verify(workflowDefinitionDao).storeWorkflowDefinition(workflowDefinition);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void definitionsAreNotStoredDuringPostProcessingWhenAutoInitIsTrue() {
    initializeService(true, true);
    service.addWorkflowDefinition(workflowDefinition);
    verify(workflowDefinitionDao).storeWorkflowDefinition(workflowDefinition);

    service.postProcessWorkflowDefinitions();

    verifyNoMoreInteractions(workflowDefinitionDao);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void definitionsAreNotStoredDuringPostProcessingWhenDefinitionPersistIsFalse() {
    initializeService(false, false);
    service.addWorkflowDefinition(workflowDefinition);

    service.postProcessWorkflowDefinitions();

    verifyNoInteractions(workflowDefinitionDao);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void addingDuplicatDefinitionThrowsException() {
    initializeService(true, true);
    service.addWorkflowDefinition(workflowDefinition);

    IllegalStateException thrown = assertThrows(IllegalStateException.class,
        () -> service.addWorkflowDefinition(workflowDefinition));

    String className = workflowDefinition.getClass().getName();
    assertThat(thrown.getMessage(),
        containsString("Both " + className + " and " + className + " define same workflow type: dummy"));
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void getWorkflowDefinitionReturnsNullWhenTypeIsNotFound() {
    initializeService(true, true);

    assertThat(service.getWorkflowDefinition("notFound"), is(nullValue()));
  }

  @Test
  public void getWorkflowDefinitionReturnsDefinitionWhenTypeIsFound() {
    initializeService(true, true);

    service.addWorkflowDefinition(workflowDefinition);

    assertThat(service.getWorkflowDefinition("dummy"), is(instanceOf(DummyTestWorkflow.class)));
  }

}
