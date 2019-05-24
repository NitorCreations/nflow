package io.nflow.engine;

import io.nflow.engine.config.db.H2DatabaseConfiguration;
import io.nflow.engine.service.DummyTestWorkflow;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.executor.WorkflowExecutor;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.jdbc.datasource.init.DatabasePopulatorUtils.execute;

public class NflowEngineTest {

    @Test
    public void test() throws InterruptedException {
        Collection<AbstractWorkflowDefinition<? extends WorkflowState>> workflowDefinitions = asList(new DummyTestWorkflow());
        NflowEngine nflowEngine = null;
        try {
            nflowEngine = new NflowEngine(dataSource(), new H2DatabaseConfiguration.H2SQLVariants(), workflowDefinitions);

            WorkflowInstance newInstance = new WorkflowInstance.Builder()
                    .setType("dummy")
                    .setNextActivation(DateTime.now())
                    .build();

            List<WorkflowExecutor> executors = nflowEngine.workflowExecutorService.getWorkflowExecutors();
            assertEquals(1, executors.size());
            nflowEngine.workflowInstanceService.insertWorkflowInstance(newInstance);

            WorkflowInstance instance1 = nflowEngine.workflowInstanceService.getWorkflowInstance(1, Set.of(WorkflowInstanceInclude.values()), 100l);
            assertNotNull(instance1);
            assertEquals("dummy", instance1.type);
            assertNotNull(instance1.nextActivation);

            Thread thread = new Thread(nflowEngine);
            thread.start();

            while (!nflowEngine.isRunning()) {
                Thread.sleep(100);
            }

            while (getInstance(nflowEngine, 1).nextActivation != null) {
                Thread.sleep(100);
            }

            // instance is processed because nextActivation is set to null
            WorkflowInstance instance2 = getInstance(nflowEngine, 1);
            assertNull(instance2.nextActivation);
        } finally {
            nflowEngine.shutdown();
        }
    }

    private WorkflowInstance getInstance(NflowEngine nflowEngine, int id) {
        return nflowEngine.workflowInstanceService.getWorkflowInstance(1, Set.of(WorkflowInstanceInclude.values()), 100l);
    }

    public static DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:enginetest;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("sa");
        createTables(dataSource);
        return dataSource;
    }

    public static void createTables(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setIgnoreFailedDrops(true);
        populator.setSqlScriptEncoding(UTF_8.name());
        populator.addScript(new ClassPathResource("scripts/db/h2.create.ddl.sql"));
        execute(populator, dataSource);
    }
}
