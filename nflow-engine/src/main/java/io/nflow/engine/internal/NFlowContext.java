package io.nflow.engine.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nflow.engine.config.NFlowConfiguration;
import io.nflow.engine.config.db.DatabaseConfiguration;
import io.nflow.engine.guice.NflowController;
import io.nflow.engine.internal.di.DI;
import io.nflow.engine.internal.storage.db.DatabaseInitializer;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.service.HealthCheckService;
import io.nflow.engine.service.MaintenanceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.concurrent.ThreadFactory;

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
       databaseConfiguration.checkDatabaseConfiguration(config, dataSource);
       di.storeProvider(DatabaseInitializer.class, () -> databaseConfiguration.nflowDatabaseInitializer(dataSource, config));
       di.store(SQLVariants.class, databaseConfiguration.sqlVariants(config));
    }

    public HealthCheckService getHealthCheckService() {
        return di.get(HealthCheckService.class);
    }

    public synchronized MaintenanceService getMaintenanceService() {
        return di.get(MaintenanceService.class);
    }

    public synchronized NflowController getNflowController() {
        return di.get(NflowController.class);
    }
}
