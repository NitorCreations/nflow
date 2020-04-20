package io.nflow.engine.internal.guice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.zaxxer.hikari.HikariDataSource;
import io.nflow.engine.config.EngineConfiguration;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.config.db.H2DatabaseConfiguration;
import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;
import io.nflow.engine.internal.executor.WorkflowLifecycle;
import io.nflow.engine.internal.storage.db.DatabaseInitializer;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.service.*;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.core.io.AbstractResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class EngineModuleTest {

  @Test
  public void testEngineConfiguration() {
    Properties props = new Properties();
    props.setProperty("nflow.db.type", "h2");
    props.setProperty("nflow.executor.thread.count", "1");
    props.setProperty("nflow.autostart", "false");

    Injector injector = Guice.createInjector(new EngineEnvironmentModule(props), new EngineModule(null, new EngineConfiguration()));

    WorkflowInstanceExecutor executor = injector.getInstance(WorkflowInstanceExecutor.class);
    assertThat(executor.getQueueRemainingCapacity(), is(2));

    AbstractResource nonSpringWorkflowsListing = injector.getInstance(Key.get(AbstractResource.class, NFlow.class));
    assertThat(nonSpringWorkflowsListing, notNullValue());

    ThreadFactory factory = injector.getInstance(Key.get(ThreadFactory.class, NFlow.class));
    assertThat(factory, instanceOf(CustomizableThreadFactory.class));
    assertThat(((CustomizableThreadFactory) factory).getThreadNamePrefix(), is("nflow-executor-"));
    assertThat(((CustomizableThreadFactory) factory).getThreadGroup().getName(), is("nflow"));

    ObjectMapper mapper = injector.getInstance(Key.get(ObjectMapper.class, NFlow.class));
    assertThat(mapper.canSerialize(DateTime.class), is(true));
    assertThat(mapper.getSerializationConfig().getDefaultPropertyInclusion().getValueInclusion(),
        is(JsonInclude.Include.NON_EMPTY));

    DataSource dataSource = injector.getInstance(Key.get(DataSource.class, NFlow.class));
    assertThat(dataSource, instanceOf(HikariDataSource.class));
    assertThat(((HikariDataSource) dataSource).getPoolName(), is("nflow"));
    assertThat(((HikariDataSource) dataSource).getDataSourceClassName(), is("org.h2.jdbcx.JdbcDataSource"));
    assertThat(((HikariDataSource) dataSource).getDataSourceProperties().get("url"), is("jdbc:h2:mem:test;TRACE_LEVEL_FILE=4"));
    assertThat(((HikariDataSource) dataSource).getMaximumPoolSize(), is(4));
    assertThat(((HikariDataSource) dataSource).getIdleTimeout(), is(600000L));
    assertThat(((HikariDataSource) dataSource).isAutoCommit(), is(true));
    assertThat(((HikariDataSource) dataSource).getMetricRegistry(), nullValue());

    SQLVariants sqlVariants = injector.getInstance(SQLVariants.class);
    assertThat(sqlVariants, instanceOf(H2DatabaseConfiguration.H2SQLVariants.class));

    injector.getInstance(Key.get(DatabaseInitializer.class, NFlow.class));
    injector.getInstance(Key.get(JdbcTemplate.class, NFlow.class));
    injector.getInstance(Key.get(NamedParameterJdbcTemplate.class, NFlow.class));
    injector.getInstance(Key.get(TransactionTemplate.class, NFlow.class));

    injector.getInstance(ArchiveService.class);
    injector.getInstance(HealthCheckService.class);
    injector.getInstance(StatisticsService.class);
    injector.getInstance(WorkflowDefinitionService.class);
    injector.getInstance(WorkflowExecutorService.class);
    injector.getInstance(WorkflowInstanceService.class);

    WorkflowLifecycle lifecycle = injector.getInstance(WorkflowLifecycle.class);
    assertThat(lifecycle.getPhase(), is(Integer.MAX_VALUE));
    assertThat(lifecycle.isAutoStartup(), is(false));
  }
}
