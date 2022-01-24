package io.nflow.engine;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute;

import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import io.nflow.engine.config.db.H2DatabaseConfiguration;
import io.nflow.engine.service.DummyTestWorkflow;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.executor.WorkflowExecutor;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;

public class NflowEngineTest {

  /**
   * Travis ci build sets env variable SPRING_PROFILES_ACTIVE=nflow.db.$DB This will enable scanning for io.nflow.engine.config.db
   * which will create an extra data source, from e.g. H2DatabaseConfiguration.java The spring setup will fail because there are
   * two competing dataSources.
   *
   * This uses system property `spring.profiles.active` to override env variable `SPRING_PROFILES_ACTIVE`.
   */
  @BeforeEach
  public void setup() {
    System.setProperty("spring.profiles.active", "nflow.db.dummy");
  }

  @AfterEach
  public void teardown() {
    System.clearProperty("spring.profiles.active");
  }

  @Test
  public void test() throws InterruptedException {
    Collection<AbstractWorkflowDefinition> workflowDefinitions = asList(new DummyTestWorkflow());
    try (NflowEngine nflowEngine = new NflowEngine(dataSource(), new H2DatabaseConfiguration.H2SQLVariants(),
        workflowDefinitions)) {
      String type = "dummy";
      String externalId = "dummy";
      WorkflowInstance newInstance = new WorkflowInstance.Builder().setType(type).setExternalId(externalId)
          .setNextActivation(DateTime.now().plusMillis(200)).build();

      List<WorkflowExecutor> executors = nflowEngine.getWorkflowExecutorService().getWorkflowExecutors();
      assertEquals(1, executors.size());
      nflowEngine.getWorkflowInstanceService().insertWorkflowInstance(newInstance);

      WorkflowInstance instance1 = getInstance(nflowEngine, type, externalId);
      assertNotNull(instance1);
      assertEquals("dummy", instance1.type);
      assertNotNull(instance1.nextActivation);

      while (getInstance(nflowEngine, type, externalId).nextActivation != null) {
        Thread.sleep(100);
      }

      // instance is processed because nextActivation is set to null
      WorkflowInstance instance2 = getInstance(nflowEngine, type, externalId);
      assertNull(instance2.nextActivation);
    }
  }

  private WorkflowInstance getInstance(NflowEngine nflowEngine, String type, String externalId) {
    QueryWorkflowInstances query = new QueryWorkflowInstances.Builder().addTypes(type).setExternalId(externalId).build();
    return nflowEngine.getWorkflowInstanceService().listWorkflowInstances(query).iterator().next();
  }

  static DataSource dataSource() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.h2.Driver");
    dataSource.setUrl("jdbc:h2:mem:enginetest;DB_CLOSE_DELAY=-1");
    dataSource.setUsername("sa");
    dataSource.setPassword("sa");
    createTables(dataSource);
    return dataSource;
  }

  static void createTables(DataSource dataSource) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setIgnoreFailedDrops(true);
    populator.setSqlScriptEncoding(UTF_8.name());
    populator.addScript(new ClassPathResource("scripts/db/h2.create.ddl.sql"));
    execute(populator, dataSource);
  }
}
