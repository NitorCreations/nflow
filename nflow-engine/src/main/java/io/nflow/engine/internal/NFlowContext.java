package io.nflow.engine.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nflow.engine.guice.NflowController;
import io.nflow.engine.internal.di.DI;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import io.nflow.engine.config.NFlowConfiguration;
import io.nflow.engine.config.db.DatabaseConfiguration;
import io.nflow.engine.internal.dao.ExecutorDao;
import io.nflow.engine.internal.dao.HealthCheckDao;
import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.internal.dao.StatisticsDao;
import io.nflow.engine.internal.dao.TableMetadataChecker;
import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;
import io.nflow.engine.internal.storage.db.DatabaseInitializer;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.service.HealthCheckService;
import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

public class NFlowContext {
    private final NFlowConfiguration config;

    private final DI di = new DI();

    public NFlowContext(NFlowConfiguration configuration) {
       this.config = di.store(NFlowConfiguration.class, configuration);
    }

    public void configure() {
       var dataSource = di.store(DataSource.class, config.getDataSource());
       var jdbcTemplate = di.store(new JdbcTemplate(dataSource, true));
       var objectMapper = di.store(ObjectMapper.class, config.getObjectMapper());
       di.store(new NamedParameterJdbcTemplate(dataSource));
       di.store(ThreadFactory.class, config.getThreadFactory());
       di.store(TransactionTemplate.class, new TransactionTemplate(config.getTransactionManager()));

       var databaseConfiguration = di.store(DatabaseConfiguration.class, config.getDatabaseConfiguration());
       di.store(SQLVariants.class, databaseConfiguration.sqlVariants(config));
       databaseConfiguration.nflowDatabaseInitializer(dataSource, config);
    }

    public HealthCheckService getHealthCheckService() {
        return di.getOrCreate(HealthCheckService.class);
    }

    public synchronized MaintenanceService getMaintenanceService() {
        return di.getOrCreate(MaintenanceService.class);
    }

    public synchronized NflowController getNflowController() {
        return di.getOrCreate(NflowController.class);
    }
}
