package io.nflow.engine;

import io.nflow.engine.config.EngineConfiguration;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.config.db.H2DatabaseConfiguration;
import io.nflow.engine.internal.executor.WorkflowDispatcher;
import io.nflow.engine.internal.storage.db.SQLVariants;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

public class NflowEngine {

    public static void main(String ... args) {
        NflowEngine e = new NflowEngine(dataSource(), new H2DatabaseConfiguration.H2SQLVariants());
        e.run();
    }

    private final WorkflowDispatcher workflowDispatcher;

    public NflowEngine(DataSource dataSource, SQLVariants sqlVariants) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        //ctx.registerBean("dataSource", DataSource.class, dataSource);
        ctx.registerBean(DataSource.class, () -> dataSource);

        ctx.register(EngineConfiguration.class, NflowEngineSpringConfig.class);

        ctx.registerBean(SQLVariants.class, () -> sqlVariants);
        ctx.refresh();
        workflowDispatcher = ctx.getBean(WorkflowDispatcher.class);
    }

    public void run() {
        workflowDispatcher.run();
    }

    public static DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("sa");
        return dataSource;
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
