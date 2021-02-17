package io.nflow.engine;

import java.util.Collection;

import javax.sql.DataSource;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import io.nflow.engine.config.EngineConfiguration;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.executor.WorkflowLifecycle;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.service.HealthCheckService;
import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.service.StatisticsService;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.service.WorkflowExecutorService;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;

/**
 * NflowEngine starts up nflow-engine with given database and workflow definitions.
 *
 * Use this if you want to start just the nflow-engine.
 */
public class NflowEngine implements AutoCloseable {

  private final AnnotationConfigApplicationContext ctx;
  private final WorkflowLifecycle workflowLifecycle;
  private final MaintenanceService maintenanceService;
  private final HealthCheckService healthCheckService;
  private final StatisticsService statisticsService;
  private final WorkflowDefinitionService workflowDefinitionService;
  private final WorkflowInstanceService workflowInstanceService;
  private final WorkflowExecutorService workflowExecutorService;

  /**
   * Starts up the NflowEngine with WorkflowDispatcher running in a thread. Property nflow.autostart controls if the thread is
   * started automatically. If nflow.autostart=false, then the thread can be started with start() method.
   *
   * @param dataSource
   *          nFlow database data source.
   * @param sqlVariants
   *          SQL variants for the configured database type.
   * @param workflowDefinitions
   *          The registered workflow definitions.
   */
  public NflowEngine(DataSource dataSource, SQLVariants sqlVariants, Collection<AbstractWorkflowDefinition> workflowDefinitions) {
    ctx = new AnnotationConfigApplicationContext();

    ctx.registerBean("nflowDatasource", DataSource.class, () -> dataSource);
    ctx.registerBean(SQLVariants.class, () -> sqlVariants);
    ctx.register(EngineConfiguration.class, NflowEngineSpringConfig.class);
    ctx.refresh();

    workflowLifecycle = ctx.getBean(WorkflowLifecycle.class);

    workflowDefinitionService = ctx.getBean(WorkflowDefinitionService.class);
    workflowDefinitions.forEach(workflowDefinitionService::addWorkflowDefinition);

    maintenanceService = ctx.getBean(MaintenanceService.class);
    healthCheckService = ctx.getBean(HealthCheckService.class);
    statisticsService = ctx.getBean(StatisticsService.class);
    workflowInstanceService = ctx.getBean(WorkflowInstanceService.class);
    workflowExecutorService = ctx.getBean(WorkflowExecutorService.class);
  }

  /**
   * For manually starting dispatcher thread. This starts the nflow-engine if property nflow.autostart=false.
   */
  public void start() {
    workflowLifecycle.start();
  }

  /**
   * Pauses a running nFlow engine.
   */
  public void pause() {
    workflowLifecycle.pause();
  }

  /**
   * Resumes a paused nFlow engine.
   */
  public void resume() {
    workflowLifecycle.resume();
  }

  /**
   * Returns true if the nFlow engine is currently paused.
   *
   * @return True if engine is currently paused.
   */
  public boolean isPaused() {
    return workflowLifecycle.isPaused();
  }

  /**
   * Returns true if the nFlow engine is currently running.
   *
   * @return True if engine is currently running.
   */
  public boolean isRunning() {
    return workflowLifecycle.isRunning();
  }

  /**
   * Shuts down the nFlow engine gracefully and returns when the shutdown is complete. This NflowEngine instance may not be
   * restarted after close().
   */
  @Override
  public void close() {
    ctx.close();
  }

  /**
   * @return ArchiveService for nFlow engine.
   */
  public MaintenanceService getMaintenanceService() {
    return maintenanceService;
  }

  /**
   * @return HealthCheckService for nFlow engine.
   */
  public HealthCheckService getHealthCheckService() {
    return healthCheckService;
  }

  /**
   * @return StatisticsService for nFlow engine.
   */
  public StatisticsService getStatisticsService() {
    return statisticsService;
  }

  /**
   * @return WorkflowDefinitionService for nFlow engine.
   */
  public WorkflowDefinitionService getWorkflowDefinitionService() {
    return workflowDefinitionService;
  }

  /**
   * @return WorkflowInstanceService for nFlow engine.
   */
  public WorkflowInstanceService getWorkflowInstanceService() {
    return workflowInstanceService;
  }

  /**
   * @return WorkflowExecutorService for nFlow engine.
   */
  public WorkflowExecutorService getWorkflowExecutorService() {
    return workflowExecutorService;
  }

  @EnableTransactionManagement
  protected static class NflowEngineSpringConfig {
    @Bean
    @NFlow
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }

    @Bean
    @NFlow
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
      return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    @NFlow
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
      return new TransactionTemplate(transactionManager);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource ds) {
      return new DataSourceTransactionManager(ds);
    }
  }
}
