package com.nitorcreations.nflow.engine.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import com.nitorcreations.nflow.engine.BaseNflowTest;
import com.nitorcreations.nflow.engine.dao.RepositoryDao;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;

public class RepositoryServiceTest extends BaseNflowTest {

  @Mock
  private RepositoryDao repositoryDao;

  @Mock
  private ApplicationContext appCtx;

  private RepositoryService service;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
    Mockito.when(appCtx.getBean(Mockito.any(Class.class))).thenThrow(NoSuchBeanDefinitionException.class);
    service = new RepositoryService(repositoryDao, appCtx);
  }

  @Test
  public void demoWorkflowLoadedSuccessfully() throws Exception {
    service.initWorkflowDefinitions();
    List<WorkflowDefinition<? extends WorkflowState>> definitions = service.getWorkflowDefinitions();
    assertThat(definitions.size(), is(0));
  }

}
