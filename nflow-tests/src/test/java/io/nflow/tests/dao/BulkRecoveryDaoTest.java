package io.nflow.tests.dao;

import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.dao.ExecutorDao;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.tests.AbstractNflowTest;
import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.NflowServerExtension.BeforeServerStop;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BulkRecoveryDaoTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder()
      .springContextClass(ServerContext.class)
      .build();

  private static JdbcTemplate jdbc;
  private static WorkflowInstanceDao workflowInstanceDao;
  private static ExecutorDao executorDao;

  public BulkRecoveryDaoTest() {
    super(server);
  }

  @Configuration
  static class ServerContext {
    @Inject
    public void init(@NFlow JdbcTemplate nflowJdbc, WorkflowInstanceDao dao, ExecutorDao executor) {
      jdbc = nflowJdbc;
      workflowInstanceDao = dao;
      executorDao = executor;
    }
  }

  @Test
  @Order(1)
  public void insertNineThousandDeadExecutorsAndOneWorkflow() {
    int count = 9000;
    Timestamp crash = new Timestamp(now().minusDays(1).getMillis());
    Timestamp active = new Timestamp(now().minusDays(1).plusSeconds(1).getMillis());
    Timestamp expires = new Timestamp(now().minusDays(1).plusHours(1).getMillis());
    List<Object[]> args = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      args.add(new Object[] { "localhost", 666 + i, executorDao.getExecutorGroup(), crash, active, expires });
    }
    jdbc.batchUpdate(
        "insert into nflow_executor (host, pid, executor_group, started, active, expires) values (?, ?, ?, ?, ?, ?)",
        args);

    int firstCrashedExecutorId = jdbc.queryForObject(
        "select min(id) from nflow_executor where executor_group = ? and expires < current_timestamp and recovered is null",
        Integer.class, executorDao.getExecutorGroup());
    jdbc.update(
        "insert into nflow_workflow (status, type, external_id, state, executor_id, executor_group, priority) values (?, ?, ?, ?, ?, ?, 0)",
        executing.name(), "bulkTest", "extId0", "processing", firstCrashedExecutorId, executorDao.getExecutorGroup());
  }

  @Test
  @Order(2)
  public void recoverWorkflowInstancesFromNineThousandDeadExecutors() {
    workflowInstanceDao.recoverWorkflowInstancesFromDeadNodes();

    int recoveredExecutors = jdbc.queryForObject(
        "select count(*) from nflow_executor where executor_group = ? and recovered is not null",
        Integer.class, executorDao.getExecutorGroup());
    assertThat(recoveredExecutors, is(9000));

    Integer workflowExecutorId = jdbc.queryForObject(
        "select executor_id from nflow_workflow where executor_group = ? and type = ?",
        Integer.class, executorDao.getExecutorGroup(), "bulkTest");
    assertThat(workflowExecutorId, is((Integer) null));
  }

  @BeforeServerStop
  public void cleanUp() {
    String group = executorDao.getExecutorGroup();
    jdbc.update("delete from nflow_workflow_action where workflow_id in (select id from nflow_workflow where executor_group = ? and type = ?)", group, "bulkTest");
    jdbc.update("delete from nflow_workflow where executor_group = ? and type = ?", group, "bulkTest");
    jdbc.update("delete from nflow_executor where executor_group = ? and expires < current_timestamp", group);
  }
}
