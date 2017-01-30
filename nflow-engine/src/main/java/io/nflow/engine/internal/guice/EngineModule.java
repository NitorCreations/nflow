package io.nflow.engine.internal.guice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.nflow.engine.config.EngineConfiguration;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.config.db.*;
import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;
import io.nflow.engine.internal.executor.WorkflowLifecycle;
import io.nflow.engine.internal.storage.db.DatabaseInitializer;
import io.nflow.engine.internal.storage.db.SQLVariants;
import org.h2.tools.Server;
import org.springframework.core.env.Environment;
import org.springframework.core.io.AbstractResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.concurrent.ThreadFactory;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

public class EngineModule extends AbstractModule {

    private final Object metricRegistry;
    private final EngineConfiguration engineConfiguration;

    public EngineModule(final Object metricRegistry, final EngineConfiguration engineConfiguration) {
        this.metricRegistry = metricRegistry;
        this.engineConfiguration = engineConfiguration;
    }

    final DataSourceTransactionManager transactionManager = new GuiceDataSourceTransactionManager();

    @Override
    protected void configure() {
        requestInjection(transactionManager);
        bindInterceptor(any(), annotatedWith(Transactional.class),
                new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        install(new EngineInitModule());
    }

    static class GuiceDataSourceTransactionManager extends DataSourceTransactionManager {
        private static final long serialVersionUID = 1L;

        @Inject
        public void setDatasource(@NFlow DataSource dataSource) {
            super.setDataSource(dataSource);
        }
    }

    @Provides
    @Inject
    @NFlow
    @Singleton
    public AbstractResource nflowNonSpringWorkflowsListing(Environment env) {
        return engineConfiguration.nflowNonSpringWorkflowsListing(env);
    }

    @Provides
    @NFlow
    @Singleton
    public ThreadFactory nflowThreadFactory() {
        return engineConfiguration.nflowThreadFactory();
    }

    @Provides
    @Inject
    @Singleton
    public WorkflowInstanceExecutor nflowExecutor(@NFlow ThreadFactory factory, Environment env) {
        return engineConfiguration.nflowExecutor(factory, env);
    }

    @Provides
    @NFlow
    @Singleton
    public ObjectMapper nflowObjectMapper() {
        return engineConfiguration.nflowObjectMapper();
    }

    @Provides
    @NFlow
    @Singleton
    public DataSource nflowDataSource(Environment env, DatabaseConfiguration databaseConfiguration) {
        return databaseConfiguration.nflowDatasource(env, metricRegistry);
    }

    @Provides
    @NFlow
    @Singleton
    @Inject
    public DatabaseInitializer nflowDatabaseInitializer(@NFlow DataSource dataSource, Environment env, DatabaseConfiguration databaseConfiguration) {
        return databaseConfiguration.nflowDatabaseInitializer(dataSource, env);
    }

    @Provides
    @NFlow
    @Singleton
    @Inject
    public JdbcTemplate nflowJdbcTemplate(@NFlow DataSource dataSource, DatabaseConfiguration databaseConfiguration) {
        return databaseConfiguration.nflowJdbcTemplate(dataSource);
    }

    @Provides
    @NFlow
    @Singleton
    @Inject
    public NamedParameterJdbcTemplate nflowNamedParameterJdbcTemplate(@NFlow DataSource dataSource, DatabaseConfiguration databaseConfiguration) {
        return databaseConfiguration.nflowNamedParameterJdbcTemplate(dataSource);
    }

    @Provides
    @NFlow
    @Singleton
    @Inject
    public TransactionTemplate nflowTransactionTemplate(@NFlow DataSourceTransactionManager transactionManager, DatabaseConfiguration databaseConfiguration) {
        return databaseConfiguration.nflowTransactionTemplate(transactionManager);
    }

    @Provides
    @NFlow
    @Singleton
    public DataSourceTransactionManager nflowPlatformTransactionManager() {
        return transactionManager;
    }

    @Provides
    @Singleton
    @Inject
    public SQLVariants nflowSQLVariants(DatabaseConfiguration databaseConfiguration) {
        return databaseConfiguration.sqlVariants();
    }

    @Provides
    @Singleton
    @Inject
    public DatabaseConfiguration databaseConfiguration(Environment env) {
        DatabaseConfiguration db;
        String dbtype = env.getProperty("nflow.db.type", String.class, "h2");
        switch (dbtype) {
            case "h2":
                db = new H2DatabaseConfiguration();
                break;
            case "mysql":
                db = new MysqlDatabaseConfiguration();
                break;
            case "oracle":
                db = new OracleDatabaseConfiguration();
                break;
            case "postgresql":
                db = new PgDatabaseConfiguration();
                break;
            default:
                throw new RuntimeException("Unknown DB");
        }
        return db;
    }

    class EngineInitModule extends AbstractModule {
        @Override
        protected void configure() {
            requestInjection(this);
        }

        @Inject
        void initLifeCycleAutoStart(WorkflowLifecycle lifecycle) {
            if (lifecycle.isAutoStartup()) {
                lifecycle.start();
            }
        }

        @Inject
        void initH2TcpServer(Environment env, DatabaseConfiguration databaseConfiguration) throws Exception {
            if (databaseConfiguration instanceof H2DatabaseConfiguration) {
                Server server = ((H2DatabaseConfiguration) databaseConfiguration).h2TcpServer(env);
                if (server != null) {
                    server.start();
                }
            }
        }

        @Inject
        void initH2ConsoleServer(Environment env, DatabaseConfiguration databaseConfiguration) throws Exception {
            if (databaseConfiguration instanceof H2DatabaseConfiguration) {
                Server server = ((H2DatabaseConfiguration) databaseConfiguration).h2ConsoleServer(env);
                if (server != null) {
                    server.start();
                }
            }
        }
    }
}
