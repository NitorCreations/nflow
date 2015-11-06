package com.nitorcreations.nflow.engine.internal.dao;

import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static java.util.Arrays.asList;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

public class ArchiveDaoTest extends BaseDaoTest {
  @Inject
  ArchiveDao archiveDao;
  @Inject
  WorkflowInstanceDao workflowInstanceDao;

  DateTime archiveTimeLimit = new DateTime(2015, 7, 8, 21, 28, 0, 0);

  DateTime archiveTime1 = archiveTimeLimit.minus(1);
  DateTime archiveTime2 = archiveTimeLimit.minusMinutes(1);
  DateTime archiveTime3 = archiveTimeLimit.minusHours(2);
  DateTime archiveTime4 = archiveTimeLimit.minusDays(3);

  DateTime prodTime1 = archiveTimeLimit.plus(1);
  DateTime prodTime2 = archiveTimeLimit.plusMinutes(1);
  DateTime prodTime3 = archiveTimeLimit.plusHours(2);
  DateTime prodTime4 = archiveTimeLimit.plusDays(3);

  // TODO implement tests for child workflows and their actions, states

  @Test
  public void listingArchivableWorkflows() {
    List<Integer> expectedArchive = new ArrayList<>();

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime1);
    storePassiveWorkflow(prodTime2);

    expectedArchive.add(storePassiveWorkflow(archiveTime1));
    expectedArchive.add(storePassiveWorkflow(archiveTime2));

    List<Integer> archivableIds = archiveDao.listArchivableWorkflows(archiveTimeLimit, 10);
    assertEqualsInAnyOrder(expectedArchive, archivableIds);
  }

  @Test
  public void listingReturnsOldestRowsAndMaxBatchSizeRows() {
    List<Integer> expectedArchive = new ArrayList<>();

    int eleventh = storePassiveWorkflow(archiveTime2);

    for (int i = 0; i < 9; i++) {
      expectedArchive.add(storePassiveWorkflow(archiveTime4));
    }
    expectedArchive.add(storePassiveWorkflow(archiveTime3));

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime3);
    storePassiveWorkflow(prodTime4);

    List<Integer> archivableIds = archiveDao.listArchivableWorkflows(archiveTimeLimit, 10);
    Collections.sort(archivableIds);
    assertEquals(expectedArchive, archivableIds);

    expectedArchive.add(eleventh);
    archivableIds = archiveDao.listArchivableWorkflows(archiveTimeLimit, 11);
    assertEqualsInAnyOrder(expectedArchive, archivableIds);
  }

  @Test
  public void archivingSimpleWorkflowsWorks() {
    List<Integer> archivableWorkflows = new ArrayList<>();

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime1);
    storePassiveWorkflow(prodTime1);

    archivableWorkflows.add(storePassiveWorkflow(archiveTime1));
    archivableWorkflows.add(storePassiveWorkflow(archiveTime2));

    int activeWorkflowCountBefore = rowCount("select 1 from nflow_workflow");
    assertEquals(archivableWorkflows.size(), archiveDao.archiveWorkflows(archivableWorkflows));
    int activeWorkflowCountAfter = rowCount("select 1 from nflow_workflow");

    assertActiveWorkflowsRemoved(archivableWorkflows);
    assertArchiveWorkflowsExist(archivableWorkflows);

    assertEquals(archivableWorkflows.size(), rowCount("select 1 from nflow_archive_workflow"));
    assertEquals(activeWorkflowCountAfter, activeWorkflowCountBefore - archivableWorkflows.size());
  }

  @Test
  public void archivingWorkflowsWithActionsWorks() {
    List<Integer> archivableWorkflows = new ArrayList<>();
    List<Integer> archivableActions = new ArrayList<>();

    storeActions(storeActiveWorkflow(archiveTime1), 3);
    storeActions(storeActiveWorkflow(prodTime1), 1);
    storeActions(storePassiveWorkflow(prodTime1), 2);

    int archivable1 = storePassiveWorkflow(archiveTime1);
    int archivable2 = storePassiveWorkflow(archiveTime2);
    archivableActions.addAll(storeActions(archivable1, 1));
    archivableActions.addAll(storeActions(archivable2, 3));

    archivableWorkflows.addAll(asList(archivable1, archivable2));

    int activeActionCountBefore = rowCount("select 1 from nflow_workflow_action");
    assertEquals(archivableWorkflows.size(), archiveDao.archiveWorkflows(archivableWorkflows));
    int activeActionCountAfter = rowCount("select 1 from nflow_workflow_action");

    assertActiveWorkflowsRemoved(archivableWorkflows);
    assertArchiveWorkflowsExist(archivableWorkflows);

    assertActiveActionsRemoved(archivableActions);
    assertArchiveActionsExist(archivableActions);

    assertEquals(archivableActions.size(), rowCount("select 1 from nflow_archive_workflow_action"));
    assertEquals(activeActionCountAfter, activeActionCountBefore - archivableActions.size());
  }

  @Test
  public void archivingWorkflowsWithActionsAndStatesWorks() {
    List<Integer> archivableWorkflows = new ArrayList<>();
    List<Integer> archivableActions = new ArrayList<>();
    List<StateKey> archivableStates = new ArrayList<>();

    int nonArchivableWorkflow1 = storeActiveWorkflow(archiveTime1);
    storeStateVariables(nonArchivableWorkflow1, storeActions(nonArchivableWorkflow1, 3), 1);

    int nonArchivableWorkflow2 = storeActiveWorkflow(prodTime1);
    storeStateVariables(nonArchivableWorkflow2, storeActions(nonArchivableWorkflow2, 1), 3);

    int nonArchivableWorkflow3 = storePassiveWorkflow(prodTime1);
    storeStateVariables(nonArchivableWorkflow3, storeActions(nonArchivableWorkflow3, 2), 2);

    int archivable1 = storePassiveWorkflow(archiveTime1);
    int archivable2 = storePassiveWorkflow(archiveTime2);
    List<Integer> actions1 = storeActions(archivable1, 1);
    List<Integer> actions2 = storeActions(archivable2, 2);

    archivableActions.addAll(actions1);
    archivableActions.addAll(actions2);

    archivableStates.addAll(storeStateVariables(archivable1, actions1, 4));
    archivableStates.addAll(storeStateVariables(archivable2, actions2, 2));

    archivableWorkflows.addAll(asList(archivable1, archivable2));

    int variablesCountBefore = rowCount("select 1 from nflow_workflow_state");
    assertEquals(archivableWorkflows.size(), archiveDao.archiveWorkflows(archivableWorkflows));
    int variablesCountAfter = rowCount("select 1 from nflow_workflow_state");

    assertActiveWorkflowsRemoved(archivableWorkflows);
    assertArchiveWorkflowsExist(archivableWorkflows);

    assertActiveActionsRemoved(archivableActions);
    assertArchiveActionsExist(archivableActions);

    assertActiveStateVariablesRemoved(archivableStates);
    assertArchiveStateVariablesExist(archivableStates);

    // each workflow gets automatically stateVariable called "requestData"
    int requestDataVariableCount = archivableWorkflows.size();
    assertEquals(archivableStates.size() + requestDataVariableCount, rowCount("select 1 from nflow_archive_workflow_state"));

    assertEquals(variablesCountAfter, variablesCountBefore - archivableStates.size() - requestDataVariableCount);
  }

  private void assertActiveWorkflowsRemoved(List<Integer> workflowIds) {
    for (int id : workflowIds) {
      try {
        workflowInstanceDao.getWorkflowInstance(id);
        fail("Expected workflow " + id + " to be removed");
      } catch (EmptyResultDataAccessException e) {
        // expected exception
      }
    }
  }

  private void assertArchiveWorkflowsExist(List<Integer> workflowIds) {
    for (int workflowId : workflowIds) {
      Map<String, Object> archived = getArchivedWorkflow(workflowId);
      assertEquals(workflowId, archived.get("id"));
    }
  }

  private void assertActiveActionsRemoved(List<Integer> actionIds) {
    for (int actionId : actionIds) {
      int found = rowCount("select 1 from nflow_workflow_action where id = ?", actionId);
      assertEquals("Found unexpected action " + actionId + " in nflow_workflow_action", 0, found);
    }
  }

  private void assertArchiveActionsExist(List<Integer> actionIds) {
    for (int actionId : actionIds) {
      int found = rowCount("select 1 from nflow_archive_workflow_action where id = ?", actionId);
      assertEquals("Action " + actionId + " not found in nflow_archive_workflow_action", 1, found);
    }
  }

  private void assertActiveStateVariablesRemoved(List<StateKey> stateKeys) {
    for (StateKey stateKey : stateKeys) {
      int found = rowCount("select 1 from nflow_workflow_state where workflow_id = ? and action_id = ? and state_key = ?",
          stateKey.workflowId, stateKey.actionId, stateKey.stateKey);
      assertEquals("Found unexpected state variable " + stateKey + " in nflow_workflow_state", 0, found);
    }
  }

  private void assertArchiveStateVariablesExist(List<StateKey> stateKeys) {
    for (StateKey stateKey : stateKeys) {
      int found = rowCount(
          "select 1 from nflow_archive_workflow_state where workflow_id = ? and action_id = ? and state_key = ?",
          stateKey.workflowId, stateKey.actionId, stateKey.stateKey);
      assertEquals("State variable " + stateKey + " not found in nflow_archive_workflow_state", 1, found);
    }
  }

  private int rowCount(String sql, Object... params) {
    return jdbc.queryForList(sql, params).size();
  }

  private Map<String, Object> getArchivedWorkflow(int workflowId) {
    return jdbc.queryForMap("select * from nflow_archive_workflow where id = ?", new Object[] { workflowId });
  }

  private int storePassiveWorkflow(DateTime modified) {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().setStatus(created).setNextActivation(null)
        .setModified(modified).build();
    int id = insert(instance);
    return id;
  }

  private int storeActiveWorkflow(DateTime modified) {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().setStatus(created).setNextActivation(now())
        .setModified(modified).build();
    int id = insert(instance);
    return id;
  }

  private List<Integer> storeActions(int workflowId, int actionCount) {
    List<Integer> actionIds = new ArrayList<>();
    for (int i = 0; i < actionCount; i++) {
      actionIds.add(storeAction(workflowId));
    }
    return actionIds;
  }

  private List<StateKey> storeStateVariables(int workflowId, List<Integer> actionIds, int count) {
    List<StateKey> stateKeys = new ArrayList<>();
    for (int actionId : actionIds) {
      stateKeys.addAll(storeStateVariables(workflowId, actionId, count));
    }
    return stateKeys;
  }

  private List<StateKey> storeStateVariables(int workflowId, int actionId, int stateCount) {
    List<StateKey> stateKeys = new ArrayList<>();
    int index = 1;
    for (int i = 0; i < stateCount; i++) {
      stateKeys.add(storeStateVariable(workflowId, actionId, "key-" + (index++)));
    }
    return stateKeys;
  }

  private StateKey storeStateVariable(int workflowId, int actionId, String key) {
    String value = key + "_value";
    int updated = jdbc.update(
        "insert into nflow_workflow_state (workflow_id, action_id, state_key, state_value) values (?, ?, ?, ?)", workflowId,
        actionId, key, value);
    assertEquals(1, updated);
    return new StateKey(workflowId, actionId, key);
  }

  private int storeAction(int workflowId) {
    WorkflowInstanceAction action = actionBuilder(workflowId).build();
    return workflowInstanceDao.insertWorkflowInstanceAction(action);
  }

  private WorkflowInstanceAction.Builder actionBuilder(int workflowId) {
    return new WorkflowInstanceAction.Builder().setState("dummyState")
        .setType(WorkflowInstanceAction.WorkflowActionType.stateExecution).setExecutionStart(DateTime.now())
        .setExecutionEnd(DateTime.now()).setWorkflowInstanceId(workflowId);
  }

  private int insert(WorkflowInstance instance) {
    int id = workflowInstanceDao.insertWorkflowInstance(instance);
    assertTrue(id > 0);
    DateTime modified = instance.modified;
    updateModified(id, modified);
    WorkflowInstance dbInstance = workflowInstanceDao.getWorkflowInstance(id);
    assertEquals(modified, dbInstance.modified);
    return id;
  }

  private void updateModified(int workflowId, DateTime modified) {
    int updateCount = jdbc.update("update nflow_workflow set modified = ? where id = ?",
        new Object[] { DaoUtil.toTimestamp(modified), workflowId });
    assertEquals(1, updateCount);
  }

  private void assertEqualsInAnyOrder(List<Integer> expected, List<Integer> actual) {
    List<Integer> expectedCopy = new ArrayList<>(expected);
    List<Integer> actualCopy = new ArrayList<>(actual);
    Collections.sort(expectedCopy);
    Collections.sort(actualCopy);
    assertEquals(expectedCopy, actualCopy);
  }

  private static class StateKey {
    public final int workflowId;
    public final int actionId;
    public final String stateKey;

    public StateKey(int workflowId, int actionId, String stateKey) {
      this.workflowId = workflowId;
      this.actionId = actionId;
      this.stateKey = stateKey;
    }

    @Override
    public String toString() {
      return ReflectionToStringBuilder.toString(this);
    }
  }
}
