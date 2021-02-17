package io.nflow.engine.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.AbstractResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.dao.ExecutorDao;
import io.nflow.engine.internal.dao.HealthCheckDao;
import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.internal.dao.StatisticsDao;
import io.nflow.engine.internal.dao.TableMetadataChecker;
import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("nflow-engine-test")
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@DirtiesContext
public class WorkflowDefinitionServiceWithSpringTest {

  @Configuration
  @Profile("nflow-engine-test")
  @ComponentScan(basePackageClasses = SpringDummyTestWorkflow.class)
  static class ContextConfiguration {
    @Bean
    @Primary
    public Environment env() {
      return new MockEnvironment().withProperty("nflow.definition.persist", "true").withProperty("nflow.autoinit", "true");
    }

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
    public WorkflowDefinitionDao workflowDefinitionDao() {
      return mock(WorkflowDefinitionDao.class);
    }

    @Bean
    public ExecutorDao executorDao() {
      return mock(ExecutorDao.class);
    }

    @Bean
    public StatisticsDao statisticsDao() {
      return mock(StatisticsDao.class);
    }

    @Bean
    public WorkflowInstancePreProcessor preProcessor() {
      return mock(WorkflowInstancePreProcessor.class);
    }

    @Bean
    public MaintenanceDao archiveDao() {
      return mock(MaintenanceDao.class);
    }

    @Bean
    public HealthCheckDao healthCheckDao() {
      return mock(HealthCheckDao.class);
    }

    @Bean
    @NFlow
    public JdbcTemplate jdbcTemplate() {
      return mock(JdbcTemplate.class);
    }

    @Bean
    @NFlow
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
      return mock(NamedParameterJdbcTemplate.class);
    }

    @Bean
    public TableMetadataChecker tableMetadataChecker() {
      return mock(TableMetadataChecker.class);
    }

    @Bean
    public SQLVariants SQLVariants() {
      return mock(SQLVariants.class);
    }

    @Bean
    @NFlow
    public ObjectMapper objectMapper() {
      return mock(ObjectMapper.class);
    }

    @Bean
    @NFlow
    public TransactionTemplate transactionTemplate() {
      return mock(TransactionTemplate.class);
    }

    @Bean
    public WorkflowInstanceExecutor workflowInstanceExecutor() {
      return mock(WorkflowInstanceExecutor.class);
    }

    @Bean
    public WorkflowInstanceFactory workflowInstanceFactory() {
      return mock(WorkflowInstanceFactory.class);
    }

  }

  @Autowired
  private WorkflowDefinitionService service;

  @SuppressWarnings("unused")
  @Autowired
  private WorkflowDefinitionSpringBeanScanner scanner;

  @Test
  public void springWorkflowDefinitionsAreDetected() {
    List<AbstractWorkflowDefinition> definitions = service.getWorkflowDefinitions();
    assertThat(definitions.size(), is(equalTo(1)));
    assertThat(definitions.get(0).getType(), is(new SpringDummyTestWorkflow().getType()));
  }

}
