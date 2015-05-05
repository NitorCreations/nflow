package com.nitorcreations.nflow.engine.service;

import static org.joda.time.DateTime.now;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Service for managing workflow instances.
 */
@Component
public class WorkflowInstanceService {

  @Inject
  private WorkflowDefinitionService workflowDefinitionService;
  @Inject
  private WorkflowInstanceDao workflowInstanceDao;

  public WorkflowInstanceService() {
  }

  public WorkflowInstanceService(WorkflowDefinitionService workflowDefinitionService, WorkflowInstanceDao workflowInstanceDao) {
    this.workflowDefinitionService = workflowDefinitionService;
    this.workflowInstanceDao = workflowInstanceDao;
  }

  /**
   * Return the workflow instance matching the given id.
   * @param id Workflow instance id.
   * @return The workflow instance, or null if not found.
   */
  public WorkflowInstance getWorkflowInstance(int id) {
    return workflowInstanceDao.getWorkflowInstance(id);
  }

  /**
   * Insert the workflow instance to the database and return the id of the
   * instance. If the instance already exists, return the id of the existing
   * instance.
   * @param instance The workflow instance to be inserted.
   * @return The id of the inserted or existing workflow instance.
   */
  @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", justification = "getInitialState().toString() has no cast")
  public int insertWorkflowInstance(WorkflowInstance instance) {
    WorkflowDefinition<?> def = workflowDefinitionService.getWorkflowDefinition(instance.type);
    if (def == null) {
      throw new RuntimeException("No workflow definition found for type [" + instance.type + "]");
    }
    WorkflowInstance.Builder builder = new WorkflowInstance.Builder(instance);
    if (instance.state == null) {
      builder.setState(def.getInitialState().toString());
    } else {
      if (!def.isStartState(instance.state)) {
        throw new RuntimeException("Specified state [" + instance.state + "] is not a start state.");
      }
    }
    if (isEmpty(instance.externalId)) {
      builder.setExternalId(UUID.randomUUID().toString());
    }
    if (instance.status == null) {
      builder.setStatus(WorkflowInstanceStatus.created);
    }
    int id = workflowInstanceDao.insertWorkflowInstance(builder.build());
    if (id == -1 && !isEmpty(instance.externalId)) {
      QueryWorkflowInstances query = new QueryWorkflowInstances.Builder().addTypes(def.getType()).setExternalId(instance.externalId).build();
      id = workflowInstanceDao.queryWorkflowInstances(query).get(0).id;
    }
    return id;
  }

  /**
   * Update the workflow instance in the database if it is currently not running, and insert the workflow instance action. If
   * action is null, it will be automatically created. If the state of the instance is not null, the status of the instance is
   * updated based on the new state. If the state of the instance is null, neither state nor status are updated.
   * @param instance The instance to be updated.
   * @param action The action to be inserted. Can be null.
   * @return True if the update was successful, false otherwise.
   */
  @Transactional
  public boolean updateWorkflowInstance(WorkflowInstance instance, WorkflowInstanceAction action) {
    WorkflowInstance.Builder builder = new WorkflowInstance.Builder(instance);
    if (instance.state == null) {
      builder.setStatus(null);
    } else {
      String type = workflowInstanceDao.getWorkflowInstance(instance.id).type;
      WorkflowDefinition<?> definition = workflowDefinitionService.getWorkflowDefinition(type);
      builder.setStatus(definition.getState(instance.state).getType().getStatus());
    }
    WorkflowInstance updatedInstance = builder.build();
    boolean updated = workflowInstanceDao.updateNotRunningWorkflowInstance(updatedInstance);
    if (updated) {
      WorkflowInstanceAction.Builder actionBuilder;
      if (action == null) {
        actionBuilder = new WorkflowInstanceAction.Builder().setWorkflowInstanceId(updatedInstance.id).setStateText("N/A");
      } else {
        actionBuilder = new WorkflowInstanceAction.Builder(action);
      }
      String currentState = workflowInstanceDao.getWorkflowInstanceState(updatedInstance.id);
      action = actionBuilder.setState(currentState).build();
      workflowInstanceDao.insertWorkflowInstanceAction(updatedInstance, action);
    }
    return updated;
  }

  /**
   * Unschedule workflow instance in the database if it is not currently executing, and insert the workflow instance action
   * if the actionDescription is not null.
   *
   * @param id The identifier of the workflow instance to be stopped.
   * @param actionDescription The action description. Can be null.
   * @param actionType The type of action.
   * @return True if the workflow instance was stopped, false otherwise.
   */
  @Transactional
  public boolean stopWorkflowInstance(int id, String actionDescription, WorkflowActionType actionType) {
    boolean updated = workflowInstanceDao.stopNotRunningWorkflowInstance(id, actionDescription);
    if (updated && actionDescription != null) {
      String currentState = workflowInstanceDao.getWorkflowInstanceState(id);
      WorkflowInstanceAction action = new WorkflowInstanceAction.Builder().setWorkflowInstanceId(id).setState(currentState)
          .setStateText(actionDescription).setExecutionStart(now()).setExecutionEnd(now()).setType(actionType).build();
      workflowInstanceDao.insertWorkflowInstanceAction(action);
    }
    return updated;
  }

  /**
   * Update workflow instance status in the database to paused if the instance is not currently executing or unscheduled, and
   * insert the workflow instance action if the actionDescription is not null.
   *
   * @param id The identifier of the workflow instance to be paused.
   * @param actionDescription The action description. Can be null.
   * @param actionType The type of action.
   * @return True if the workflow instance was paused, false otherwise.
   */
  @Transactional
  public boolean pauseWorkflowInstance(int id, String actionDescription, WorkflowActionType actionType) {
    boolean updated = workflowInstanceDao.pauseNotRunningWorkflowInstance(id, actionDescription);
    if (updated && actionDescription != null) {
      String currentState = workflowInstanceDao.getWorkflowInstanceState(id);
      WorkflowInstanceAction action = new WorkflowInstanceAction.Builder().setWorkflowInstanceId(id).setState(currentState)
          .setStateText(actionDescription).setExecutionStart(now()).setExecutionEnd(now()).setType(actionType).build();
      workflowInstanceDao.insertWorkflowInstanceAction(action);
    }
    return updated;
  }

  /**
   * Resume workflow instance in the database if it is paused, and insert the workflow instance action if the actionDescription
   * is not null.
   *
   * @param id The identifier of the workflow instance to be resumed.
   * @param actionDescription The action description. Can be null.
   * @param actionType The type of action.
   * @return True if the workflow instance was resumed, false otherwise.
   */
  @Transactional
  public boolean resumeWorkflowInstance(int id, String actionDescription, WorkflowActionType actionType) {
    boolean updated = workflowInstanceDao.resumePausedWorkflowInstance(id, actionDescription);
    if (updated && actionDescription != null) {
      String currentState = workflowInstanceDao.getWorkflowInstanceState(id);
      WorkflowInstanceAction action = new WorkflowInstanceAction.Builder().setWorkflowInstanceId(id).setState(currentState)
          .setStateText(actionDescription).setExecutionStart(now()).setExecutionEnd(now()).setType(actionType).build();
      workflowInstanceDao.insertWorkflowInstanceAction(action);
    }
    return updated;
  }

  /**
   * Wake up the workflow instance matching the given id if it is in one of the expected states.
   * @param id Workflow instance id.
   * @param expectedStates The expected states.
   * @return True if the instance was woken up, false otherwise.
   */
  @Transactional
  public boolean wakeupWorkflowInstance(long id, String... expectedStates) {
    return workflowInstanceDao.wakeupWorkflowInstanceIfNotExecuting(id, expectedStates);
  }

  /**
   * Return workflow instances matching the given query.
   * @param query The query parameters.
   * @return Matching workflow instances, or empty collection if none found.
   */
  public Collection<WorkflowInstance> listWorkflowInstances(QueryWorkflowInstances query) {
    return workflowInstanceDao.queryWorkflowInstances(query);
  }
}
