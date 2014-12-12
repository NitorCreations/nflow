package com.nitorcreations.nflow.engine.service;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.AbstractResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.nitorcreations.nflow.engine.internal.config.NFlow;
import com.nitorcreations.nflow.engine.internal.dao.ExecutorDao;
import com.nitorcreations.nflow.engine.internal.dao.StatisticsDao;
import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class WorkflowDefinitionServiceWithSpringTest {

  @Configuration
  @ComponentScan(basePackageClasses = SpringDummyTestWorkflow.class)
  static class ContextConfiguration {

    @Bean
    @NFlow
    public AbstractResource nflowNonSpringWorkflowsListing() {
      return null;
    }

    @Bean
    public WorkflowInstanceDao workflowInstanceDao() {
      return mock(WorkflowInstanceDao.class);
    }

    @Bean
    public ExecutorDao executorDao() {
      return mock(ExecutorDao.class);
    }

    @Bean
    public StatisticsDao statisticsDao() {
      return mock(StatisticsDao.class);
    }
  }

  @Autowired
  private WorkflowDefinitionService service;

  @Test
  public void springWorkflowDefinitionsAreDetected() {
    List<WorkflowDefinition<? extends WorkflowState>> definitions = service.getWorkflowDefinitions();
    assertThat(definitions.size(), is(equalTo(1)));
    assertThat(definitions.get(0).getType(), is(new SpringDummyTestWorkflow().getType()));
  }
}
