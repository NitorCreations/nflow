package io.nflow.engine;

import io.nflow.engine.config.EngineConfiguration;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.executor.WorkflowDispatcher;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.service.*;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Collection;

/**
 * NflowEngine starts up nflow-engine with given database and workflow definitions.
 *
 * Use this if you want to start just the nflow-engine.
 */
public class NflowEngine implements Runnable {

    private final WorkflowDispatcher workflowDispatcher;

    public final ArchiveService archiveService;
    public final HealthCheckService healthCheckService;
    public final StatisticsService statisticsService;
    public final WorkflowDefinitionService workflowDefinitionService;
    public final WorkflowInstanceService workflowInstanceService;
    public final WorkflowExecutorService workflowExecutorService;

    public NflowEngine(DataSource dataSource,
                       SQLVariants sqlVariants,
                       Collection<AbstractWorkflowDefinition<? extends WorkflowState>> workflowDefinitions) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

        ctx.registerBean("nflowDatasource", DataSource.class, () -> dataSource);
        ctx.registerBean(SQLVariants.class, () -> sqlVariants);
        ctx.register(EngineConfiguration.class, NflowEngineSpringConfig.class);
        ctx.refresh();

        workflowDispatcher = ctx.getBean(WorkflowDispatcher.class);

        workflowDefinitionService = ctx.getBean(WorkflowDefinitionService.class);
        workflowDefinitionService.setWorkflowDefinitions(workflowDefinitions);

        archiveService = ctx.getBean(ArchiveService.class);
        healthCheckService = ctx.getBean(HealthCheckService.class);
        statisticsService = ctx.getBean(StatisticsService.class);
        workflowInstanceService = ctx.getBean(WorkflowInstanceService.class);
        workflowExecutorService = ctx.getBean(WorkflowExecutorService.class);
    }

    public void run() {
        workflowDispatcher.run();
    }

    public void shutdown() {
        workflowDispatcher.shutdown();
    }

    public void pause() {
        workflowDispatcher.pause();
    }

    public void resume() {
        workflowDispatcher.resume();
    }

    public boolean isPaused() {
        return workflowDispatcher.isPaused();
    }

    public boolean isRunning() {
        return workflowDispatcher.isRunning();
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
