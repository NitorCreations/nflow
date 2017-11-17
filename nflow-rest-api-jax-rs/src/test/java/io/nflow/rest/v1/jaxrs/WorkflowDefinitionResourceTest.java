package io.nflow.rest.v1.jaxrs;

import static com.nitorcreations.Matchers.containsElements;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.internal.workflow.StoredWorkflowDefinition;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.rest.v1.converter.ListWorkflowDefinitionConverter;
import io.nflow.rest.v1.jaxrs.WorkflowDefinitionResource;
import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowDefinitionResourceTest {

  @Mock
  private WorkflowDefinitionService workflowDefinitions;
  @Mock
  private WorkflowDefinitionDao workflowDefinitionDao;
  @Mock
  private ListWorkflowDefinitionConverter converter;
  @Captor
  private ArgumentCaptor<Collection<String>> stringList;
  @Mock
  private WorkflowDefinition<? extends WorkflowState> dummyDefinition;
  @Mock
  private ListWorkflowDefinitionResponse dummyResponse;

  private WorkflowDefinitionResource resource;

  @Before
  public void setup() {
    when(dummyDefinition.getType()).thenReturn("dummy");
    doReturn(asList(dummyDefinition)).when(workflowDefinitions).getWorkflowDefinitions();
    when(converter.convert(dummyDefinition)).thenReturn(dummyResponse);
    dummyResponse.type = "dummy";
    resource = new WorkflowDefinitionResource(workflowDefinitions, converter, workflowDefinitionDao);
  }

  @Test
  public void listWorkflowDefinitionsFindsExistingDefinition() {
    Collection<ListWorkflowDefinitionResponse> ret = resource.listWorkflowDefinitions(asList("dummy"));
    assertThat(ret.size(), is(1));
    verify(workflowDefinitionDao, never()).queryStoredWorkflowDefinitions(anyCollection());
  }

  @Test
  public void listWorkflowDefinitionsDoesNotFindNonExistentDefinition() {
    Collection<ListWorkflowDefinitionResponse> ret = resource.listWorkflowDefinitions(asList("nonexistent"));
    assertThat(ret.size(), is(0));
    verify(workflowDefinitionDao).queryStoredWorkflowDefinitions(stringList.capture());
    assertThat(stringList.getValue(), containsElements(asList("nonexistent")));
  }

  @Test
  public void listWorkflowDefinitionsFindsExistingDefinitionWithoutArguments() {
    Collection<ListWorkflowDefinitionResponse> ret = resource.listWorkflowDefinitions(Collections.<String>emptyList());
    assertThat(ret.size(), is(1));
    verify(workflowDefinitionDao).queryStoredWorkflowDefinitions(stringList.capture());
    assertThat(stringList.getValue().size(), is(0));
  }

  @Test
  public void listWorkflowDefinitionsFindsExistingAndStoredDefinitionsWithoutArguments() {
    StoredWorkflowDefinition storedDefinitionDummy = mock(StoredWorkflowDefinition.class);
    StoredWorkflowDefinition storedDefinitionNew = mock(StoredWorkflowDefinition.class);
    when(workflowDefinitionDao.queryStoredWorkflowDefinitions(anyCollection())).thenReturn(
        asList(storedDefinitionDummy, storedDefinitionNew));
    ListWorkflowDefinitionResponse storedResponseDummy = mock(ListWorkflowDefinitionResponse.class, "dbDummy");
    ListWorkflowDefinitionResponse storedResponseNew = mock(ListWorkflowDefinitionResponse.class, "dbNew");
    storedDefinitionDummy.type = "dummy";
    when(converter.convert(storedDefinitionNew)).thenReturn(storedResponseNew);
    storedDefinitionNew.type = "new";
    Collection<ListWorkflowDefinitionResponse> ret = resource.listWorkflowDefinitions(Collections.<String>emptyList());
    assertThat(ret, hasItems(storedResponseNew, dummyResponse));
    assertThat(ret, not(hasItem(storedResponseDummy)));
  }

  @Test
  public void listWorkflowDefinitionsFindsExistingAndStoredDefinitionsWithDbType() {
    StoredWorkflowDefinition storedDefinitionNew = mock(StoredWorkflowDefinition.class);
    when(workflowDefinitionDao.queryStoredWorkflowDefinitions(anyCollection())).thenReturn(
        asList(storedDefinitionNew));
    ListWorkflowDefinitionResponse storedResponseNew = mock(ListWorkflowDefinitionResponse.class, "dbNew");
    when(converter.convert(storedDefinitionNew)).thenReturn(storedResponseNew);
    storedDefinitionNew.type = "new";
    Collection<ListWorkflowDefinitionResponse> ret = resource.listWorkflowDefinitions(asList("new"));
    assertThat(ret, hasItems(storedResponseNew));
  }
}
