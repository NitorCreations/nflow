package io.nflow.engine.workflow;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import io.nflow.engine.internal.executor.BaseNflowTest;
import io.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import io.nflow.engine.service.DummyTestWorkflow;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.instance.WorkflowInstance;

public class WorkflowInstancePreProcessorTest extends BaseNflowTest {

  @Mock
  private WorkflowDefinitionService workflowDefinitionService;

  private WorkflowInstancePreProcessor preProcessor;

  @BeforeEach
  public void setup() {
    WorkflowDefinition<?> dummyWorkflow = new DummyTestWorkflow();
    lenient().doReturn(dummyWorkflow).when(workflowDefinitionService).getWorkflowDefinition("dummy");
    preProcessor = new WorkflowInstancePreProcessor(workflowDefinitionService);
  }

  @Test
  public void wrongStartStateCausesException() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setExternalId("123").setState("end").build();
    RuntimeException thrown = assertThrows(RuntimeException.class, () -> preProcessor.process(i));
    assertThat(thrown.getMessage(), containsString("Specified state [end] is not a start state."));
  }

  @Test
  public void createsMissingExternalId() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().build();
    WorkflowInstance processed = preProcessor.process(i);
    assertThat(processed.externalId, notNullValue());
  }

  @Test
  public void createsMissingState() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().build();
    WorkflowInstance processed = preProcessor.process(i);
    assertThat(processed.state, is("CreateLoan"));
  }

  @Test
  public void setsStatusToCreatedWhenStatusIsNotSpecified() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setStatus(null).build();
    WorkflowInstance processed = preProcessor.process(i);
    assertThat(processed.status, is(WorkflowInstance.WorkflowInstanceStatus.created));
  }

  @Test
  public void unsupportedTypeThrowsException() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setType("nonexistent").build();
    RuntimeException thrown = assertThrows(RuntimeException.class, () -> preProcessor.process(i));
    assertThat(thrown.getMessage(), containsString("No workflow definition found for type [nonexistent]"));
  }
}
