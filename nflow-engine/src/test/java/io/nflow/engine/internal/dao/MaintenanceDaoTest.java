package io.nflow.engine.internal.dao;

import static io.nflow.engine.internal.dao.MaintenanceDao.TablePrefix.MAIN;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;

import io.nflow.engine.model.ModelObject;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;

public class MaintenanceDaoTest extends BaseDaoTest {
  @Inject
  MaintenanceDao maintenanceDao;
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
    List<Long> expectedArchive = new ArrayList<>();

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime1);
    storePassiveWorkflow(prodTime2);

    expectedArchive.add(storePassiveWorkflow(archiveTime1));
    expectedArchive.add(storePassiveWorkflow(archiveTime2));

    List<Long> archivableIds = maintenanceDao.listOldWorkflows(MAIN, archiveTimeLimit, 10);
    assertThat(archivableIds, containsInAnyOrder(expectedArchive.toArray()));
  }

  @Test
  public void listingReturnsOldestRowsAndMaxBatchSizeRows() {
    List<Long> expectedArchive = new ArrayList<>();

    long eleventh = storePassiveWorkflow(archiveTime2);

    for (int i = 0; i < 9; i++) {
      expectedArchive.add(storePassiveWorkflow(archiveTime4));
    }
    expectedArchive.add(storePassiveWorkflow(archiveTime3));

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime3);
    storePassiveWorkflow(prodTime4);

    List<Long> archivableIds = maintenanceDao.listOldWorkflows(MAIN, archiveTimeLimit, 10);
    assertThat(archivableIds, containsInAnyOrder(expectedArchive.toArray()));

    expectedArchive.add(eleventh);
    archivableIds = maintenanceDao.listOldWorkflows(MAIN, archiveTimeLimit, 11);
    assertThat(archivableIds, containsInAnyOrder(expectedArchive.toArray()));
  }

  @Test
  public void archivingSimpleWorkflowsWorks() {
    List<Long> archivableWorkflows = new ArrayList<>();

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime1);
    storePassiveWorkflow(prodTime1);

    archivableWorkflows.add(storePassiveWorkflow(archiveTime1));
    archivableWorkflows.add(storePassiveWorkflow(archiveTime2));

    int activeWorkflowCountBefore = rowCount("select 1 from nflow_workflow");
    assertEquals(archivableWorkflows.size(), maintenanceDao.archiveWorkflows(archivableWorkflows));
    int activeWorkflowCountAfter = rowCount("select 1 from nflow_workflow");

    assertActiveWorkflowsRemoved(archivableWorkflows);
    assertArchiveWorkflowsExist(archivableWorkflows);

    assertEquals(archivableWorkflows.size(), rowCount("select 1 from nflow_archive_workflow"));
    assertEquals(activeWorkflowCountAfter, activeWorkflowCountBefore - archivableWorkflows.size());
  }

  @Test
  public void archivingWorkflowsWithActionsWorks() {
    List<Long> archivableWorkflows = new ArrayList<>();
    List<Long> archivableActions = new ArrayList<>();

    storeActions(storeActiveWorkflow(archiveTime1), 3);
    storeActions(storeActiveWorkflow(prodTime1), 1);
    storeActions(storePassiveWorkflow(prodTime1), 2);

    long archivable1 = storePassiveWorkflow(archiveTime1);
    long archivable2 = storePassiveWorkflow(archiveTime2);
    archivableActions.addAll(storeActions(archivable1, 1));
    archivableActions.addAll(storeActions(archivable2, 3));

    archivableWorkflows.addAll(asList(archivable1, archivable2));

    int activeActionCountBefore = rowCount("select 1 from nflow_workflow_action");
    assertEquals(archivableWorkflows.size(), maintenanceDao.archiveWorkflows(archivableWorkflows));
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
    List<Long> archivableWorkflows = new ArrayList<>();
    List<Long> archivableActions = new ArrayList<>();
    List<StateKey> archivableStates = new ArrayList<>();

    long nonArchivableWorkflow1 = storeActiveWorkflow(archiveTime1);
    storeStateVariables(nonArchivableWorkflow1, storeActions(nonArchivableWorkflow1, 3), 1);

    long nonArchivableWorkflow2 = storeActiveWorkflow(prodTime1);
    storeStateVariables(nonArchivableWorkflow2, storeActions(nonArchivableWorkflow2, 1), 3);

    long nonArchivableWorkflow3 = storePassiveWorkflow(prodTime1);
    storeStateVariables(nonArchivableWorkflow3, storeActions(nonArchivableWorkflow3, 2), 2);

    long archivable1 = storePassiveWorkflow(archiveTime1);
    long archivable2 = storePassiveWorkflow(archiveTime2);
    List<Long> actions1 = storeActions(archivable1, 1);
    List<Long> actions2 = storeActions(archivable2, 2);

    archivableActions.addAll(actions1);
    archivableActions.addAll(actions2);

    archivableStates.addAll(storeStateVariables(archivable1, actions1, 4));
    archivableStates.addAll(storeStateVariables(archivable2, actions2, 2));

    archivableWorkflows.addAll(asList(archivable1, archivable2));

    int variablesCountBefore = rowCount("select 1 from nflow_workflow_state");
    assertEquals(archivableWorkflows.size(), maintenanceDao.archiveWorkflows(archivableWorkflows));
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

  private void assertActiveWorkflowsRemoved(List<Long> workflowIds) {
    for (long id : workflowIds) {
      try {
        workflowInstanceDao.getWorkflowInstance(id, emptySet(), null);
        fail("Expected workflow " + id + " to be removed");
      } catch (@SuppressWarnings("unused") EmptyResultDataAccessException e) {
        // expected exception
      }
    }
  }

  private void assertArchiveWorkflowsExist(List<Long> workflowIds) {
    for (long workflowId : workflowIds) {
      Map<String, Object> archived = getArchivedWorkflow(workflowId);
      assertEquals(workflowId, ((Number) archived.get("id")).longValue());
    }
  }

  private void assertActiveActionsRemoved(List<Long> actionIds) {
    for (long actionId : actionIds) {
      int found = rowCount("select 1 from nflow_workflow_action where id = ?", actionId);
      assertEquals(0, found, "Found unexpected action " + actionId + " in nflow_workflow_action");
    }
  }

  private void assertArchiveActionsExist(List<Long> actionIds) {
    for (long actionId : actionIds) {
      int found = rowCount("select 1 from nflow_archive_workflow_action where id = ?", actionId);
      assertEquals(1, found, "Action " + actionId + " not found in nflow_archive_workflow_action");
    }
  }

  private void assertActiveStateVariablesRemoved(List<StateKey> stateKeys) {
    for (StateKey stateKey : stateKeys) {
      int found = rowCount("select 1 from nflow_workflow_state where workflow_id = ? and action_id = ? and state_key = ?",
          stateKey.workflowId, stateKey.actionId, stateKey.stateKey);
      assertEquals(0, found, "Found unexpected state variable " + stateKey + " in nflow_workflow_state");
    }
  }

  private void assertArchiveStateVariablesExist(List<StateKey> stateKeys) {
    for (StateKey stateKey : stateKeys) {
      int found = rowCount(
          "select 1 from nflow_archive_workflow_state where workflow_id = ? and action_id = ? and state_key = ?",
          stateKey.workflowId, stateKey.actionId, stateKey.stateKey);
      assertEquals(1, found, "State variable " + stateKey + " not found in nflow_archive_workflow_state");
    }
  }

  private int rowCount(String sql, Object... params) {
    return jdbc.queryForList(sql, params).size();
  }

  private Map<String, Object> getArchivedWorkflow(long workflowId) {
    return jdbc.queryForMap("select * from nflow_archive_workflow where id = ?", workflowId);
  }

  private long storePassiveWorkflow(DateTime modified) {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().setStatus(created).setNextActivation(null)
        .setModified(modified).build();
    long id = insert(instance);
    return id;
  }

  private long storeActiveWorkflow(DateTime modified) {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().setStatus(created).setModified(modified).build();
    long id = insert(instance);
    return id;
  }

  private List<Long> storeActions(long workflowId, int actionCount) {
    List<Long> actionIds = new ArrayList<>();
    for (int i = 0; i < actionCount; i++) {
      actionIds.add(storeAction(workflowId));
    }
    return actionIds;
  }

  private List<StateKey> storeStateVariables(long workflowId, List<Long> actionIds, int count) {
    List<StateKey> stateKeys = new ArrayList<>();
    for (long actionId : actionIds) {
      stateKeys.addAll(storeStateVariables(workflowId, actionId, count));
    }
    return stateKeys;
  }

  private List<StateKey> storeStateVariables(long workflowId, long actionId, int stateCount) {
    List<StateKey> stateKeys = new ArrayList<>();
    int index = 1;
    for (int i = 0; i < stateCount; i++) {
      stateKeys.add(storeStateVariable(workflowId, actionId, "key-" + (index++)));
    }
    return stateKeys;
  }

  private StateKey storeStateVariable(long workflowId, long actionId, String key) {
    String value = key + "_value";
    int updated = jdbc.update(
        "insert into nflow_workflow_state (workflow_id, action_id, state_key, state_value) values (?, ?, ?, ?)", workflowId,
        actionId, key, value);
    assertEquals(1, updated);
    return new StateKey(workflowId, actionId, key);
  }

  private long storeAction(long workflowId) {
    WorkflowInstanceAction action = actionBuilder(workflowId).build();
    return workflowInstanceDao.insertWorkflowInstanceAction(action);
  }

  private WorkflowInstanceAction.Builder actionBuilder(long workflowId) {
    return new WorkflowInstanceAction.Builder().setState("dummyState")
        .setType(WorkflowInstanceAction.WorkflowActionType.stateExecution).setExecutionStart(DateTime.now())
        .setExecutionEnd(DateTime.now()).setWorkflowInstanceId(workflowId);
  }

  private long insert(WorkflowInstance instance) {
    long id = workflowInstanceDao.insertWorkflowInstance(instance);
    assertTrue(id > 0);
    DateTime modified = instance.modified;
    updateModified(id, modified);
    WorkflowInstance dbInstance = workflowInstanceDao.getWorkflowInstance(id, emptySet(), null);
    assertEquals(modified, dbInstance.modified);
    return id;
  }

  private void updateModified(long workflowId, DateTime modified) {
    int updateCount = jdbc.update("update nflow_workflow set modified = ? where id = ?",
        DaoUtil.toTimestamp(modified), workflowId);
    assertEquals(1, updateCount);
  }

  private static class StateKey extends ModelObject {
    public final long workflowId;
    public final long actionId;
    public final String stateKey;

    public StateKey(long workflowId, long actionId, String stateKey) {
      this.workflowId = workflowId;
      this.actionId = actionId;
      this.stateKey = stateKey;
    }
  }
}
