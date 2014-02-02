package com.nitorcreations.nflow.engine.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.nitorcreations.nflow.engine.BaseNflowTest;
import com.nitorcreations.nflow.engine.dao.RepositoryDao;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;

public class RepositoryServiceTest extends BaseNflowTest {

  @Mock
  private RepositoryDao repositoryDao;
  
  private RepositoryService service;
  
  @Before
  public void setup() throws Exception {
    service = new RepositoryService(repositoryDao);
  }
  
  @Test
  public void demoWorkflowLoadedSuccessfully() {
    List<WorkflowDefinition<? extends WorkflowState>> definitions = service.getWorkflowDefinitions();
    assertThat(definitions.size(), is(1));
    assertThat(definitions.get(0).getType(), is("demo"));
  }
  
}
