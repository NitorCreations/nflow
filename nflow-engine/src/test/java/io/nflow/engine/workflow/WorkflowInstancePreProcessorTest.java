package io.nflow.engine.workflow;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.executor.BaseNflowTest;
import io.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import io.nflow.engine.service.DummyTestWorkflow;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.TestState;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.instance.WorkflowInstance;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowInstancePreProcessorTest extends BaseNflowTest {

  @Mock
  private WorkflowDefinitionService workflowDefinitionService;

  @Mock
  private WorkflowInstanceDao workflowInstanceDao;

  private WorkflowInstancePreProcessor preProcessor;

  private AbstractWorkflowDefinition dummyWorkflow;

  private static final short DEFAULT_PRIORITY = 100;

  @BeforeEach
  public void setup() {
    dummyWorkflow = new DummyTestWorkflow(new WorkflowSettings.Builder().setDefaultPriority(DEFAULT_PRIORITY).build());
    lenient().doReturn(dummyWorkflow).when(workflowDefinitionService).getWorkflowDefinition(DummyTestWorkflow.DUMMY_TYPE);
    preProcessor = new WorkflowInstancePreProcessor(workflowDefinitionService, workflowInstanceDao);
  }

  @Test
  public void wrongStartStateCausesException() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setExternalId("123").setState(TestState.DONE.name()).build();
    RuntimeException thrown = assertThrows(RuntimeException.class, () -> preProcessor.process(i));
    assertThat(thrown.getMessage(), containsString("Specified state [done] is not a start state."));
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
    assertThat(processed.state, is(TestState.BEGIN.name()));
  }

  @Test
  public void checksStateVariableValues() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().putStateVariable("foo", "bar").build();
    preProcessor.process(i);
    verify(workflowInstanceDao).checkStateVariableValueLength("foo", "bar");
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

  @Test
  public void setsPriorityToDefinitionDefaultIfMissing() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setPriority(null).build();
    WorkflowInstance processed = preProcessor.process(i);
    assertThat(processed.priority, is(DEFAULT_PRIORITY));
  }

  @Test
  public void doesNotOverrideInstancePriority() {
    short priority = 10;
    WorkflowInstance i = constructWorkflowInstanceBuilder().setPriority(priority).build();
    WorkflowInstance processed = preProcessor.process(i);
    assertThat(processed.priority, is(priority));
  }
}
