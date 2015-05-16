package com.nitorcreations.nflow.engine.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowDefinitionDao;
import com.nitorcreations.nflow.engine.internal.executor.BaseNflowTest;
import com.nitorcreations.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;

public class WorkflowDefinitionServiceTest extends BaseNflowTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Mock
  private ClassPathResource nonSpringWorkflowListing;
  @Mock
  private WorkflowDefinitionDao workflowDefinitionDao;
  @Mock
  private Environment env;
  private WorkflowDefinitionService service;

  @Before
  public void setup() throws Exception {
    when(env.getRequiredProperty("nflow.definition.persist", Boolean.class)).thenReturn(true);
    String dummyTestClassname = DummyTestWorkflow.class.getName();
    ByteArrayInputStream bis = new ByteArrayInputStream(dummyTestClassname.getBytes(UTF_8));
    when(nonSpringWorkflowListing.getInputStream()).thenReturn(bis);
    service = new WorkflowDefinitionService(nonSpringWorkflowListing, workflowDefinitionDao, env);
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(0)));
    service.postProcessWorkflowDefinitions();
    assertThat(service.getWorkflowDefinitions().size(), is(equalTo(1)));
    verify(workflowDefinitionDao).storeWorkflowDefinition(eq(service.getWorkflowDefinitions().get(0)));
  }

  @Test
  public void initDuplicateWorkflows() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Both com.nitorcreations.nflow.engine.service.DummyTestWorkflow and com.nitorcreations.nflow.engine.service.DummyTestWorkflow define same workflow type: dummy");
    String dummyTestClassname = DummyTestWorkflow.class.getName();
    ByteArrayInputStream bis = new ByteArrayInputStream((dummyTestClassname + "\n" + dummyTestClassname).getBytes(UTF_8));
    when(nonSpringWorkflowListing.getInputStream()).thenReturn(bis);
    service.postProcessWorkflowDefinitions();
  }

  @Test
  public void demoWorkflowLoadedSuccessfully() {
    List<AbstractWorkflowDefinition<? extends WorkflowState>> definitions = service.getWorkflowDefinitions();
    assertThat(definitions.size(), is(equalTo(1)));
  }

  @Test
  public void getWorkflowDefinitionReturnsNullWhenTypeIsNotFound() {
    assertThat(service.getWorkflowDefinition("notFound"), is(nullValue()));
  }

  @Test
  public void getWorkflowDefinitionReturnsDefinitionWhenTypeIsFound() {
    assertThat(service.getWorkflowDefinition("dummy"), is(instanceOf(DummyTestWorkflow.class)));
  }

  @Test
  public void nonSpringWorkflowsAreOptional() throws Exception {
    service = new WorkflowDefinitionService(null, workflowDefinitionDao, env);
    service.postProcessWorkflowDefinitions();
    assertEquals(0, service.getWorkflowDefinitions().size());
  }
}
