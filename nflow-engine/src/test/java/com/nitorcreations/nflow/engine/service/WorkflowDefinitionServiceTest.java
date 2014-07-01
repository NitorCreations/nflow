package com.nitorcreations.nflow.engine.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.core.io.ClassPathResource;

import com.nitorcreations.nflow.engine.internal.executor.BaseNflowTest;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;

public class WorkflowDefinitionServiceTest extends BaseNflowTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Mock
  private ClassPathResource nonSpringWorkflowListing;
  private WorkflowDefinitionService service;

  @Before
  public void setup() throws Exception {
    String dummyTestClassname = DummyTestWorkflow.class.getName();
    ByteArrayInputStream bis = new ByteArrayInputStream(dummyTestClassname.getBytes(UTF_8));
    when(nonSpringWorkflowListing.getInputStream()).thenReturn(bis);
    service = new WorkflowDefinitionService(nonSpringWorkflowListing);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(0)));
    service.initNonSpringWorkflowDefinitions();
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
  }

  @Test
  public void initDuplicateWorkflows() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Both com.nitorcreations.nflow.engine.service.DummyTestWorkflow and com.nitorcreations.nflow.engine.service.DummyTestWorkflow define same workflow type: dummy");
    String dummyTestClassname = DummyTestWorkflow.class.getName();
    ByteArrayInputStream bis = new ByteArrayInputStream((dummyTestClassname + "\n" + dummyTestClassname).getBytes(UTF_8));
    when(nonSpringWorkflowListing.getInputStream()).thenReturn(bis);
    service.initNonSpringWorkflowDefinitions();
  }

  @Test
  public void springWorkflowsWork() {
    List<WorkflowDefinition<? extends WorkflowState>> definitions = service.getWorkflowDefinitions();
    List<WorkflowDefinition<? extends WorkflowState>> list = new ArrayList<>();
    list.add(new SpringDummyTestWorkflow());
    service.setWorkflowDefinitions(list);
    assertThat(definitions.size(), is(equalTo(1)));
  }

  @Test
  public void demoWorkflowLoadedSuccessfully() {
    List<WorkflowDefinition<? extends WorkflowState>> definitions = service.getWorkflowDefinitions();
    assertThat(definitions.size(), is(equalTo(1)));
  }

  @Test
  public void nonSpringWorkflowsAreOptional() throws Exception {
    service = new WorkflowDefinitionService(null);
    service.initNonSpringWorkflowDefinitions();
    assertEquals(0, service.getWorkflowDefinitions().size());
  }

}
