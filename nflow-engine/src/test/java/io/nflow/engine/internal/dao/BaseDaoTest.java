package io.nflow.engine.internal.dao;

import static io.nflow.engine.config.Profiles.H2;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.joda.time.DateTime.now;
import static org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute;

import jakarta.inject.Inject;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.nflow.engine.internal.executor.BaseNflowTest;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={DaoTestConfiguration.class})
@ActiveProfiles(H2)
public abstract class BaseDaoTest extends BaseNflowTest {

  @Inject
  protected DataSource ds;

  @Inject
  protected JdbcTemplate jdbc;

  protected final DateTime crashedNodeStartTime = now().minusDays(1);

  @AfterEach
  public void truncateDb() {
    ResourceDatabasePopulator populator = populator();
    populator.addScript(new ClassPathResource("scripts/db/h2.truncate.sql"));
    execute(populator, ds);
  }

  private static ResourceDatabasePopulator populator() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setIgnoreFailedDrops(true);
    populator.setSqlScriptEncoding(UTF_8.name());
    return populator;
  }

  protected void insertCrashedExecutor(int crashedExecutorId, String executorGroup) {
    jdbc.update(
        "insert into nflow_executor (id, host, pid, executor_group, started, active, expires) values (?, ?, ?, ?, ?, ?, ?)",
        crashedExecutorId, "localhost", 666, executorGroup, crashedNodeStartTime.toDate(),
        crashedNodeStartTime.plusSeconds(1).toDate(), crashedNodeStartTime.plusHours(1).toDate());
  }

}
