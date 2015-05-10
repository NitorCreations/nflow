package com.nitorcreations.nflow.performance.testdata;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.springframework.jdbc.core.JdbcTemplate;

import com.nitorcreations.nflow.engine.internal.storage.db.SQLVariants;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

public class TestDataBatchInserter {

  private final JdbcTemplate jdbcTemplate;
  private final SQLVariants sqlVariants;

  public TestDataBatchInserter(JdbcTemplate jdbcTemplate, SQLVariants sqlVariants) {
    this.jdbcTemplate = jdbcTemplate;
    this.sqlVariants = sqlVariants;
  }

  public void batchInsert(final List<WorkflowInstance> instances) {
    List<Object[]> instancesBatch = new ArrayList<>();
    List<Object[]> actionsBatch = new ArrayList<>();
    List<Object[]> statesBatch = new ArrayList<>();
    for (WorkflowInstance instance : instances) {
      instancesBatch.add(new Object[] { instance.id, instance.status.name(), instance.type, instance.businessKey, instance.externalId,
          instance.state, instance.stateText, toTimestamp(instance.nextActivation), instance.executorId, instance.retries,
          toTimestamp(instance.created), toTimestamp(instance.modified), instance.executorGroup });
      for (WorkflowInstanceAction action : instance.actions) {
        actionsBatch.add(new Object[] { action.id, action.workflowInstanceId, action.executorId,
            action.type.name(), action.state, action.stateText, action.retryNo, toTimestamp(action.executionStart),
            toTimestamp(action.executionEnd) });
        for (Entry<String, String> entry : action.updatedStateVariables.entrySet()) {
          statesBatch.add(new Object[] { instance.id, action.id, entry.getKey(), entry.getValue() });
        }
      }
    }
    jdbcTemplate.batchUpdate(
        "insert into nflow_workflow(id, status, type, business_key, external_id, state, "
            + "state_text, next_activation, executor_id, retries, created, modified, executor_group) values (?, "
            + sqlVariants.workflowStatus() + ", ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", instancesBatch);
    jdbcTemplate.batchUpdate(
        "insert into nflow_workflow_action(id, workflow_id, executor_id, type, state, state_text, retry_no, execution_start, "
            + "execution_end) values (?, ?, ?," + sqlVariants.actionType() + ", ?, ?, ?, ?, ?)",
        actionsBatch);
    jdbcTemplate.batchUpdate(
        "insert into nflow_workflow_state(workflow_id, action_id, state_key, state_value) values (?, ?, ?, ?)", statesBatch);
  }

  public int getMaxValueFromColumn(String tableName, String columnName) {
    Integer id = jdbcTemplate.queryForObject("select max(" + columnName + ") from " + tableName, Integer.class);
    if (id == null) {
      id = 0;
    }
    return id;
  }

  public static Timestamp toTimestamp(DateTime time) {
    return time == null ? null : new Timestamp(time.getMillis());
  }

}
