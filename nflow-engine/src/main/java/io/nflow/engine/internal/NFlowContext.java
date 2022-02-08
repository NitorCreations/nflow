package io.nflow.engine.internal;

import java.util.concurrent.ThreadFactory;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import io.nflow.engine.config.NFlowConfiguration;
import io.nflow.engine.internal.dao.ExecutorDao;
import io.nflow.engine.internal.dao.HealthCheckDao;
import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.internal.dao.StatisticsDao;
import io.nflow.engine.internal.dao.TableMetadataChecker;
import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.service.HealthCheckService;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

public class NFlowContext {
    private final NFlowConfiguration config;
    private JdbcTemplate jdbcTemplate;
    private HealthCheckService healthCheckService;

    public NFlowContext(NFlowConfiguration configuration) {
       this.config = configuration;
    }

    public void configure() {
       DataSource dataSource = config.getDataSource();
       jdbcTemplate = new JdbcTemplate(dataSource, true);
       new TableMetadataChecker(jdbcTemplate);

       SQLVariants sqlVariant = config.getSQLVariants();
       ExecutorDao executorDao = new ExecutorDao(sqlVariant, jdbcTemplate, config);
       new StatisticsDao(jdbcTemplate, executorDao);

       NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
       new MaintenanceDao(sqlVariant, jdbcTemplate, executorDao, namedParameterJdbcTemplate);

       ObjectMapper objectMapper = config.getObjectMapper();
       new WorkflowDefinitionDao(sqlVariant, namedParameterJdbcTemplate, objectMapper, executorDao);

       ThreadFactory threadFactory = config.getThreadFactory();
       WorkflowInstanceExecutor workflowInstanceExecutor = new WorkflowInstanceExecutor(threadFactory, config);

       ObjectStringMapper objectStringMapper = new ObjectStringMapper(objectMapper);
       WorkflowInstanceFactory workflowInstanceFactory = new WorkflowInstanceFactory(objectStringMapper);

       TransactionTemplate transactionTemplate = new TransactionTemplate(config.getTransactionManager());
       new WorkflowInstanceDao(sqlVariant, jdbcTemplate, transactionTemplate, namedParameterJdbcTemplate, executorDao, workflowInstanceExecutor, workflowInstanceFactory, config);
    }

    public synchronized HealthCheckService getHealthCheckService() {
        if (healthCheckService == null) {
            healthCheckService = new HealthCheckService(new HealthCheckDao(jdbcTemplate));
        }
        return healthCheckService;
    }
}
