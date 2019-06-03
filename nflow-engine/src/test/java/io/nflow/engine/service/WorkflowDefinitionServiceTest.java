package io.nflow.engine.service;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
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
    initializeService(true);
  }

  private void initializeService(boolean autoPersistDefinition) {
    when(env.getRequiredProperty("nflow.definition.autopersist", Boolean.class)).thenReturn(autoPersistDefinition);
    service = new WorkflowDefinitionService(workflowDefinitionDao, env);
  }

  @Test
  public void addedDefinitionIsStoredWhenAutoPersistDefinitionIsTrue() {
    service.addWorkflowDefinition(workflowDefinition);

    verify(workflowDefinitionDao).storeWorkflowDefinition(workflowDefinition);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void addedDefinitionIsNotStoredWhenAutoPersistDefinitionIsFalse() {
    initializeService(false);

    service.addWorkflowDefinition(workflowDefinition);

    verifyZeroInteractions(workflowDefinitionDao);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void persistWorkflowDefinitionStoresDefinitionsWhenAutoPersistDefinitionIsFalse() {
    initializeService(false);
    service.addWorkflowDefinition(workflowDefinition);

    service.persistWorkflowDefinitions();

    verify(workflowDefinitionDao).storeWorkflowDefinition(workflowDefinition);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void persistWorkflowDefinitionDoesNotStoreDefinitionsWhenAutoPersistDefinitionIsTrue() {
    service.addWorkflowDefinition(workflowDefinition);
    verify(workflowDefinitionDao).storeWorkflowDefinition(workflowDefinition);

    service.persistWorkflowDefinitions();

    verifyNoMoreInteractions(workflowDefinitionDao);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void addingDuplicatDefinitionThrowsException() {
    service.addWorkflowDefinition(workflowDefinition);
    verify(workflowDefinitionDao).storeWorkflowDefinition(workflowDefinition);

    IllegalStateException thrown = assertThrows(IllegalStateException.class,
        () -> service.addWorkflowDefinition(workflowDefinition));

    String className = workflowDefinition.getClass().getName();
    assertThat(thrown.getMessage(),
        containsString("Both " + className + " and " + className + " define same workflow type: dummy"));
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
    verifyNoMoreInteractions(workflowDefinitionDao);
  }

  @Test
  public void getWorkflowDefinitionReturnsDefinitionWhenTypeIsFound() {
    service.addWorkflowDefinition(workflowDefinition);

    assertThat(service.getWorkflowDefinition("dummy"), is(instanceOf(DummyTestWorkflow.class)));
  }

  @Test
  public void getWorkflowDefinitionReturnsNullWhenTypeIsNotFound() {
    assertThat(service.getWorkflowDefinition("dummy"), is(nullValue()));
  }

}
