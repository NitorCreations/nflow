package io.nflow.engine.internal.dao;

import static io.nflow.engine.internal.dao.TablePrefix.ARCHIVE;
import static io.nflow.engine.internal.dao.TablePrefix.MAIN;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import io.nflow.engine.model.ModelObject;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;

public class MaintenanceDaoTest extends BaseDaoTest {
  @Inject
  private MaintenanceDao maintenanceDao;
  @Inject
  private WorkflowInstanceDao workflowInstanceDao;
  @Inject
  private TransactionTemplate transaction;

  private final DateTime archiveTimeLimit = new DateTime(2015, 7, 8, 21, 28, 0, 0);

  private final DateTime archiveTime1 = archiveTimeLimit.minus(1);
  private final DateTime archiveTime2 = archiveTimeLimit.minusMinutes(1);
  private final DateTime archiveTime3 = archiveTimeLimit.minusHours(2);
  private final DateTime archiveTime4 = archiveTimeLimit.minusDays(3);

  private final DateTime prodTime1 = archiveTimeLimit.plus(1);
  private final DateTime prodTime2 = archiveTimeLimit.plusMinutes(1);
  private final DateTime prodTime3 = archiveTimeLimit.plusHours(2);
  private final DateTime prodTime4 = archiveTimeLimit.plusDays(3);

  @Test
  public void listOldWorkflowsReturnPassiveWorkflowsModifiedBeforeGivenTimeOrderedById() {
    List<Long> expectedIds = new ArrayList<>();

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime1);
    storePassiveWorkflow(prodTime2);

    expectedIds.add(storePassiveWorkflow(archiveTime1));
    expectedIds.add(storePassiveWorkflow(archiveTime2));

    List<Long> oldWorkflowIds = maintenanceDao.getOldWorkflowIds(MAIN, archiveTimeLimit, 10, emptySet());
    assertArrayEquals(oldWorkflowIds.toArray(), expectedIds.toArray());
  }

  @Test
  public void listOldWorkflowsReturnsRequestedNumberOfItemsOrderedById() {
    List<Long> expectedIds = new ArrayList<>();
    expectedIds.add(storePassiveWorkflow(archiveTime2));
    for (int i = 0; i < 9; i++) {
      expectedIds.add(storePassiveWorkflow(archiveTime4));
    }
    long eleventh = storePassiveWorkflow(archiveTime3);

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime3);
    storePassiveWorkflow(prodTime4);

    List<Long> oldWorkflowIds = maintenanceDao.getOldWorkflowIds(MAIN, archiveTimeLimit, 10, emptySet());
    assertArrayEquals(oldWorkflowIds.toArray(), expectedIds.toArray());

    expectedIds.add(eleventh);
    oldWorkflowIds = maintenanceDao.getOldWorkflowIds(MAIN, archiveTimeLimit, 11, emptySet());
    assertArrayEquals(oldWorkflowIds.toArray(), expectedIds.toArray());
  }

  @Test
  public void archiveWorkflowsWorks() {
    List<Long> workflowIds = new ArrayList<>();

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime1);
    storePassiveWorkflow(prodTime1);

    workflowIds.add(storePassiveWorkflow(archiveTime1));
    workflowIds.add(storePassiveWorkflow(archiveTime2));

    int workflowCountBefore = getActiveWorkflowCount();
    assertEquals(workflowIds.size(), maintenanceDao.archiveWorkflows(workflowIds));
    int workflowCountAfter = getActiveWorkflowCount();

    assertActiveWorkflowsRemoved(workflowIds);
    assertArchiveWorkflowsExist(workflowIds);

    assertEquals(workflowIds.size(), getArchivedWorkflowCount());
    assertEquals(workflowCountAfter, workflowCountBefore - workflowIds.size());
  }

  @Test
  public void deleteWorkflowsFromMainTablesWorks() {
    List<Long> workflowIds = new ArrayList<>();

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime1);
    storePassiveWorkflow(prodTime1);

    workflowIds.add(storePassiveWorkflow(archiveTime1));
    workflowIds.add(storePassiveWorkflow(archiveTime2));

    int workflowCountBefore = getActiveWorkflowCount();
    assertEquals(workflowIds.size(), maintenanceDao.deleteWorkflows(MAIN, workflowIds));
    int workflowCountAfter = getActiveWorkflowCount();

    assertActiveWorkflowsRemoved(workflowIds);
    assertArchivedWorkflowsDoNotExist(workflowIds);

    assertEquals(0, getArchivedWorkflowCount());
    assertEquals(workflowCountAfter, workflowCountBefore - workflowIds.size());
  }

  @Test
  public void deleteWorkflowsFromArchiveTablesWorks() {
    List<Long> workflowIds = new ArrayList<>();
    workflowIds.add(storePassiveWorkflow(archiveTime1));
    workflowIds.add(storePassiveWorkflow(archiveTime2));
    assertEquals(workflowIds.size(), maintenanceDao.archiveWorkflows(workflowIds));
    assertEquals(0, maintenanceDao.deleteWorkflows(MAIN, workflowIds));

    int workflowCountBefore = getArchivedWorkflowCount();
    assertEquals(workflowIds.size(), workflowCountBefore);
    assertEquals(workflowIds.size(), maintenanceDao.deleteWorkflows(ARCHIVE, workflowIds));
    int workflowCountAfter = getArchivedWorkflowCount();

    assertArchivedWorkflowsDoNotExist(workflowIds);
    assertEquals(workflowCountAfter, workflowCountBefore - workflowIds.size());
  }

  @Test
  public void archiveWorkflowsWithActionsWorks() {
    List<Long> workflowIds = new ArrayList<>();
    List<Long> actionIds = new ArrayList<>();

    storeActions(storeActiveWorkflow(archiveTime1), 3);
    storeActions(storeActiveWorkflow(prodTime1), 1);
    storeActions(storePassiveWorkflow(prodTime1), 2);

    long archivable1 = storePassiveWorkflow(archiveTime1);
    long archivable2 = storePassiveWorkflow(archiveTime2);
    actionIds.addAll(storeActions(archivable1, 1));
    actionIds.addAll(storeActions(archivable2, 3));

    workflowIds.addAll(asList(archivable1, archivable2));

    int actionCountBefore = getActiveActionCount();
    assertEquals(workflowIds.size(), maintenanceDao.archiveWorkflows(workflowIds));
    int actionCountAfter = getActiveActionCount();

    assertActiveWorkflowsRemoved(workflowIds);
    assertArchiveWorkflowsExist(workflowIds);

    assertActiveActionsRemoved(actionIds);
    assertArchiveActionsExist(actionIds, true);

    assertEquals(actionIds.size(), getArchiveActionCount());
    assertEquals(actionCountAfter, actionCountBefore - actionIds.size());
  }

  @Test
  public void deleteWorkflowsWithActionsFromMainTablesWorks() {
    List<Long> workflowIds = new ArrayList<>();
    List<Long> actionIds = new ArrayList<>();

    storeActions(storeActiveWorkflow(archiveTime1), 3);
    storeActions(storeActiveWorkflow(prodTime1), 1);
    storeActions(storePassiveWorkflow(prodTime1), 2);

    long archivable1 = storePassiveWorkflow(archiveTime1);
    long archivable2 = storePassiveWorkflow(archiveTime2);
    actionIds.addAll(storeActions(archivable1, 1));
    actionIds.addAll(storeActions(archivable2, 3));

    workflowIds.addAll(asList(archivable1, archivable2));

    int actionCountBefore = getActiveActionCount();
    assertEquals(workflowIds.size(), maintenanceDao.deleteWorkflows(MAIN, workflowIds));
    int actionCountAfter = getActiveActionCount();

    assertActiveWorkflowsRemoved(workflowIds);
    assertArchivedWorkflowsDoNotExist(workflowIds);

    assertActiveActionsRemoved(actionIds);
    assertArchiveActionsExist(actionIds, false);

    assertEquals(0, getArchiveActionCount());
    assertEquals(actionCountAfter, actionCountBefore - actionIds.size());
  }

  @Test
  public void deleteWorkflowsWithActionsFromArchiveTablesWorks() {
    List<Long> workflowIds = new ArrayList<>();
    List<Long> actionIds = new ArrayList<>();
    long archivable1 = storePassiveWorkflow(archiveTime1);
    long archivable2 = storePassiveWorkflow(archiveTime2);
    actionIds.addAll(storeActions(archivable1, 1));
    actionIds.addAll(storeActions(archivable2, 3));
    workflowIds.addAll(asList(archivable1, archivable2));
    assertEquals(workflowIds.size(), maintenanceDao.archiveWorkflows(workflowIds));
    assertEquals(0, maintenanceDao.deleteWorkflows(MAIN, workflowIds));

    int actionCountBefore = getArchiveActionCount();
    assertEquals(actionIds.size(), actionCountBefore);
    assertEquals(workflowIds.size(), maintenanceDao.deleteWorkflows(ARCHIVE, workflowIds));
    int actionCountAfter = getArchiveActionCount();

    assertArchivedWorkflowsDoNotExist(workflowIds);
    assertArchiveActionsExist(actionIds, false);
    assertEquals(actionCountAfter, actionCountBefore - actionIds.size());
  }

  @Test
  public void archiveWorkflowsWithActionsAndStatesWorks() {
    List<Long> workflowIds = new ArrayList<>();
    List<Long> actionIds = new ArrayList<>();
    List<StateKey> stateIds = new ArrayList<>();

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

    actionIds.addAll(actions1);
    actionIds.addAll(actions2);

    stateIds.addAll(storeStateVariables(archivable1, actions1, 4));
    stateIds.addAll(storeStateVariables(archivable2, actions2, 2));

    workflowIds.addAll(asList(archivable1, archivable2));

    int variablesCountBefore = getActiveStateCount();
    assertEquals(workflowIds.size(), maintenanceDao.archiveWorkflows(workflowIds));
    int variablesCountAfter = getActiveStateCount();

    assertActiveWorkflowsRemoved(workflowIds);
    assertArchiveWorkflowsExist(workflowIds);

    assertActiveActionsRemoved(actionIds);
    assertArchiveActionsExist(actionIds, true);

    assertActiveStateVariablesRemoved(stateIds);
    assertArchiveStateVariablesExist(stateIds, true);

    // each workflow gets automatically stateVariable called "requestData"
    int requestDataVariableCount = workflowIds.size();
    assertEquals(stateIds.size() + requestDataVariableCount, getArchivedStateCount());

    assertEquals(variablesCountAfter, variablesCountBefore - stateIds.size() - requestDataVariableCount);
  }

  @Test
  public void deleteWorkflowsWithActionsAndStatesFromMainTablesWorks() {
    List<Long> workflowIds = new ArrayList<>();
    List<Long> actionIds = new ArrayList<>();
    List<StateKey> stateIds = new ArrayList<>();

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

    actionIds.addAll(actions1);
    actionIds.addAll(actions2);

    stateIds.addAll(storeStateVariables(archivable1, actions1, 4));
    stateIds.addAll(storeStateVariables(archivable2, actions2, 2));

    workflowIds.addAll(asList(archivable1, archivable2));

    int variablesCountBefore = getActiveStateCount();
    assertEquals(workflowIds.size(), maintenanceDao.deleteWorkflows(MAIN, workflowIds));
    int variablesCountAfter = getActiveStateCount();

    assertActiveWorkflowsRemoved(workflowIds);
    assertArchivedWorkflowsDoNotExist(workflowIds);

    assertActiveActionsRemoved(actionIds);
    assertArchiveActionsExist(actionIds, false);

    assertActiveStateVariablesRemoved(stateIds);
    assertArchiveStateVariablesExist(stateIds, false);

    assertEquals(0, getArchivedStateCount());
    // each workflow gets automatically stateVariable called "requestData"
    int requestDataVariableCount = workflowIds.size();
    assertEquals(variablesCountAfter, variablesCountBefore - stateIds.size() - requestDataVariableCount);
  }

  @Test
  public void deleteWorkflowsWithActionsAndStatesFromArchiveTablesWorks() {
    List<Long> workflowIds = new ArrayList<>();
    List<Long> actionIds = new ArrayList<>();
    List<StateKey> stateIds = new ArrayList<>();

    long archivable1 = storePassiveWorkflow(archiveTime1);
    long archivable2 = storePassiveWorkflow(archiveTime2);
    List<Long> actions1 = storeActions(archivable1, 1);
    List<Long> actions2 = storeActions(archivable2, 2);

    actionIds.addAll(actions1);
    actionIds.addAll(actions2);

    stateIds.addAll(storeStateVariables(archivable1, actions1, 4));
    stateIds.addAll(storeStateVariables(archivable2, actions2, 2));

    workflowIds.addAll(asList(archivable1, archivable2));
    assertEquals(workflowIds.size(), maintenanceDao.archiveWorkflows(workflowIds));
    assertEquals(0, maintenanceDao.deleteWorkflows(MAIN, workflowIds));

    int variablesCountBefore = getArchivedStateCount();
    // each workflow gets automatically stateVariable called "requestData"
    int requestDataVariableCount = workflowIds.size();
    assertEquals(stateIds.size() + requestDataVariableCount, variablesCountBefore);
    assertEquals(workflowIds.size(), maintenanceDao.deleteWorkflows(ARCHIVE, workflowIds));
    int variablesCountAfter = getArchivedStateCount();

    assertArchivedWorkflowsDoNotExist(workflowIds);
    assertArchiveActionsExist(actionIds, false);
    assertArchiveStateVariablesExist(stateIds, false);
    assertEquals(variablesCountAfter, variablesCountBefore - stateIds.size() - requestDataVariableCount);
  }

  @Test
  public void archiveChildWorkflowWithActiveParentWorks() {
    List<Long> archivableWorkflows = new ArrayList<>();

    long parentId = storeActiveWorkflow(archiveTime1);
    storeActiveChildWorkflow(archiveTime1, parentId);

    archivableWorkflows.add(storePassiveChildWorkflow(archiveTime1, parentId));

    int activeWorkflowCountBefore = getActiveWorkflowCount();
    assertEquals(archivableWorkflows.size(), maintenanceDao.archiveWorkflows(archivableWorkflows));
    int activeWorkflowCountAfter = getActiveWorkflowCount();

    assertActiveWorkflowsRemoved(archivableWorkflows);
    assertArchiveWorkflowsExist(archivableWorkflows);

    assertEquals(archivableWorkflows.size(), getArchivedWorkflowCount());
    assertEquals(activeWorkflowCountAfter, activeWorkflowCountBefore - archivableWorkflows.size());
  }

  @Test
  public void archiveParentWorkflowWithActiveChildWorks() {
    List<Long> archivableWorkflows = new ArrayList<>();

    long parentId = storeActiveWorkflow(archiveTime1);
    archivableWorkflows.add(parentId);
    storeActiveChildWorkflow(archiveTime1, parentId);

    int activeWorkflowCountBefore = getActiveWorkflowCount();
    assertEquals(archivableWorkflows.size(), maintenanceDao.archiveWorkflows(archivableWorkflows));
    int activeWorkflowCountAfter = getActiveWorkflowCount();

    assertActiveWorkflowsRemoved(archivableWorkflows);
    assertArchiveWorkflowsExist(archivableWorkflows);

    assertEquals(archivableWorkflows.size(), getArchivedWorkflowCount());
    assertEquals(activeWorkflowCountAfter, activeWorkflowCountBefore - archivableWorkflows.size());
  }

  @Test
  public void deleteExpiredWorkflowHistory() {
    WorkflowInstance parentWorkflow = constructWorkflowInstanceBuilder().build();
    long parentWorkflowId = workflowInstanceDao.insertWorkflowInstance(parentWorkflow);
    long addChildActionId = addWorkflowAction(parentWorkflowId, new WorkflowInstance.Builder(parentWorkflow).build(), now(),
        now());
    WorkflowInstance childWorkflow = constructWorkflowInstanceBuilder().setParentWorkflowId(parentWorkflowId)
        .setParentActionId(addChildActionId).build();
    long childWorkflowId = workflowInstanceDao.insertWorkflowInstance(childWorkflow);
    addWorkflowAction(parentWorkflowId,
        new WorkflowInstance.Builder(parentWorkflow).putStateVariable("variable", "deletedValue").build(), now(),
        now().minusDays(1));
    addWorkflowAction(parentWorkflowId,
        new WorkflowInstance.Builder(parentWorkflow).putStateVariable("variable", "preservedValue").build(), now(), now());

    maintenanceDao.deleteActionAndStateHistory(parentWorkflowId, now());

    parentWorkflow = workflowInstanceDao.getWorkflowInstance(parentWorkflowId, EnumSet.allOf(WorkflowInstanceInclude.class),
        null);
    assertThat(parentWorkflow.getStateVariable("requestData"), equalTo("{ \"parameter\": \"abc\" }"));
    assertThat(parentWorkflow.getStateVariable("variable"), equalTo("preservedValue"));
    assertThat(parentWorkflow.actions.size(), equalTo(2));
    childWorkflow = workflowInstanceDao.getWorkflowInstance(childWorkflowId, emptySet(), null);
    assertThat(childWorkflow.parentWorkflowId, equalTo(parentWorkflowId));
  }

  private int getActiveWorkflowCount() {
    return rowCount("select 1 from nflow_workflow");
  }

  private int getActiveActionCount() {
    return rowCount("select 1 from nflow_workflow_action");
  }

  private int getActiveStateCount() {
    return rowCount("select 1 from nflow_workflow_state");
  }

  private int getArchivedWorkflowCount() {
    return rowCount("select 1 from nflow_archive_workflow");
  }

  private int getArchiveActionCount() {
    return rowCount("select 1 from nflow_archive_workflow_action");
  }

  private int getArchivedStateCount() {
    return rowCount("select 1 from nflow_archive_workflow_state");
  }

  private long addWorkflowAction(long workflowId, final WorkflowInstance instance, DateTime started, DateTime ended) {
    final WorkflowInstanceAction action = new WorkflowInstanceAction.Builder().setExecutionStart(started).setExecutorId(42)
        .setExecutionEnd(ended).setRetryNo(1).setType(stateExecution).setState("test").setStateText("state text")
        .setWorkflowInstanceId(workflowId).build();
    return transaction
        .execute((TransactionCallback<Long>) status -> workflowInstanceDao.insertWorkflowInstanceAction(instance, action));
  }

  private void assertActiveWorkflowsRemoved(List<Long> workflowIds) {
    for (long id : workflowIds) {
      assertThrows(EmptyResultDataAccessException.class, () -> workflowInstanceDao.getWorkflowInstance(id, emptySet(), null));
    }
  }

  private void assertArchiveWorkflowsExist(List<Long> workflowIds) {
    for (long workflowId : workflowIds) {
      Map<String, Object> archived = getArchivedWorkflow(workflowId);
      assertEquals(workflowId, ((Number) archived.get("id")).longValue());
    }
  }

  private void assertArchivedWorkflowsDoNotExist(List<Long> workflowIds) {
    for (long workflowId : workflowIds) {
      assertThrows(EmptyResultDataAccessException.class, () -> getArchivedWorkflow(workflowId));
    }
  }

  private void assertActiveActionsRemoved(List<Long> actionIds) {
    for (long actionId : actionIds) {
      int found = rowCount("select 1 from nflow_workflow_action where id = ?", actionId);
      assertEquals(0, found, "Found unexpected action " + actionId + " in nflow_workflow_action");
    }
  }

  private void assertArchiveActionsExist(List<Long> actionIds, boolean shouldExist) {
    for (long actionId : actionIds) {
      int found = rowCount("select 1 from nflow_archive_workflow_action where id = ?", actionId);
      if (shouldExist) {
        assertEquals(1, found, "Action " + actionId + " not found in nflow_archive_workflow_action");
      } else {
        assertEquals(0, found, "Action " + actionId + " was found in nflow_archive_workflow_action");
      }
    }
  }

  private void assertActiveStateVariablesRemoved(List<StateKey> stateKeys) {
    for (StateKey stateKey : stateKeys) {
      int found = rowCount("select 1 from nflow_workflow_state where workflow_id = ? and action_id = ? and state_key = ?",
          stateKey.workflowId, stateKey.actionId, stateKey.stateKey);
      assertEquals(0, found, "Found unexpected state variable " + stateKey + " in nflow_workflow_state");
    }
  }

  private void assertArchiveStateVariablesExist(List<StateKey> stateKeys, boolean shouldExist) {
    for (StateKey stateKey : stateKeys) {
      int found = rowCount("select 1 from nflow_archive_workflow_state where workflow_id = ? and action_id = ? and state_key = ?",
          stateKey.workflowId, stateKey.actionId, stateKey.stateKey);
      if (shouldExist) {
        assertEquals(1, found, "State variable " + stateKey + " not found in nflow_archive_workflow_state");
      } else {
        assertEquals(0, found, "State variable " + stateKey + " was found in nflow_archive_workflow_state");
      }
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
    return insert(instance);
  }

  private long storeActiveWorkflow(DateTime modified) {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().setStatus(created).setModified(modified).build();
    return insert(instance);
  }

  private long storeActiveChildWorkflow(DateTime modified, long parentId) {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().setStatus(created).setModified(modified)
        .setParentWorkflowId(parentId).build();
    return insert(instance);
  }

  private long storePassiveChildWorkflow(DateTime modified, long parentId) {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().setStatus(created).setModified(modified)
        .setParentWorkflowId(parentId).setNextActivation(null).build();
    return insert(instance);
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
    for (int i = 0; i < stateCount; i++) {
      stateKeys.add(storeStateVariable(workflowId, actionId, "key-" + (i + 1)));
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
    int updateCount = jdbc.update("update nflow_workflow set modified = ? where id = ?", DaoUtil.toTimestamp(modified),
        workflowId);
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
