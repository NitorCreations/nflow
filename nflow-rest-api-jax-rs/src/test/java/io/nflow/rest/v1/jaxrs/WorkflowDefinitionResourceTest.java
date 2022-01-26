package io.nflow.rest.v1.jaxrs;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.internal.workflow.StoredWorkflowDefinition;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.rest.v1.converter.ListWorkflowDefinitionConverter;
import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;

@ExtendWith(MockitoExtension.class)
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
  private AbstractWorkflowDefinition dummyDefinition;
  @Mock
  private ListWorkflowDefinitionResponse dummyResponse;

  private WorkflowDefinitionResource resource;
  GenericType<List<ListWorkflowDefinitionResponse>> definitionListType = new GenericType<List<ListWorkflowDefinitionResponse>>() { /**/ };

  @BeforeEach
  public void setup() {
    when(dummyDefinition.getType()).thenReturn("dummy");
    doReturn(asList(dummyDefinition)).when(workflowDefinitions).getWorkflowDefinitions();
    lenient().when(converter.convert(dummyDefinition)).thenReturn(dummyResponse);
    dummyResponse.type = "dummy";
    resource = new WorkflowDefinitionResource(workflowDefinitions, converter, workflowDefinitionDao);
  }

  @Test
  public void listWorkflowDefinitionsFindsExistingDefinition() {
    List<ListWorkflowDefinitionResponse> ret = getDefinitionListType(asList("dummy"));
    assertThat(ret.size(), is(1));
    verify(workflowDefinitionDao, never()).queryStoredWorkflowDefinitions(anyCollection());
  }

  @Test
  public void listWorkflowDefinitionsDoesNotFindNonExistentDefinition() {
    List<ListWorkflowDefinitionResponse> ret = getDefinitionListType(asList("nonexistent"));
    assertThat(ret.size(), is(0));
    verify(workflowDefinitionDao).queryStoredWorkflowDefinitions(stringList.capture());
    assertThat(stringList.getValue(), contains("nonexistent"));
  }

  @Test
  public void listWorkflowDefinitionsFindsExistingDefinitionWithoutArguments() {
    List<ListWorkflowDefinitionResponse> ret = getDefinitionListType(emptyList());
    assertThat(ret.size(), is(1));
    verify(workflowDefinitionDao).queryStoredWorkflowDefinitions(stringList.capture());
    assertThat(stringList.getValue().size(), is(0));
  }

  @Test
  public void listWorkflowDefinitionsFindsExistingAndStoredDefinitionsWithoutArguments() {
    StoredWorkflowDefinition storedDefinitionDummy = mock(StoredWorkflowDefinition.class);
    StoredWorkflowDefinition storedDefinitionNew = mock(StoredWorkflowDefinition.class);
    when(workflowDefinitionDao.queryStoredWorkflowDefinitions(anyCollection()))
        .thenReturn(asList(storedDefinitionDummy, storedDefinitionNew));
    ListWorkflowDefinitionResponse storedResponseDummy = mock(ListWorkflowDefinitionResponse.class, "dbDummy");
    ListWorkflowDefinitionResponse storedResponseNew = mock(ListWorkflowDefinitionResponse.class, "dbNew");
    storedDefinitionDummy.type = "dummy";
    when(converter.convert(storedDefinitionNew)).thenReturn(storedResponseNew);
    storedDefinitionNew.type = "new";
    Collection<ListWorkflowDefinitionResponse> ret = getDefinitionListType(emptyList());
    assertThat(ret, hasItems(storedResponseNew, dummyResponse));
    assertThat(ret, not(hasItem(storedResponseDummy)));
  }

  @Test
  public void listWorkflowDefinitionsFindsExistingAndStoredDefinitionsWithDbType() {
    StoredWorkflowDefinition storedDefinitionNew = mock(StoredWorkflowDefinition.class);
    when(workflowDefinitionDao.queryStoredWorkflowDefinitions(anyCollection())).thenReturn(asList(storedDefinitionNew));
    ListWorkflowDefinitionResponse storedResponseNew = mock(ListWorkflowDefinitionResponse.class, "dbNew");
    when(converter.convert(storedDefinitionNew)).thenReturn(storedResponseNew);
    storedDefinitionNew.type = "new";
    List<ListWorkflowDefinitionResponse> ret = getDefinitionListType(asList("new"));
    assertThat(ret, hasItems(storedResponseNew));
  }

  private List<ListWorkflowDefinitionResponse> getDefinitionListType(List<String> list) {
    try (Response r = resource.listWorkflowDefinitions(list)) {
      return r.readEntity(definitionListType);
    }
  }
}
