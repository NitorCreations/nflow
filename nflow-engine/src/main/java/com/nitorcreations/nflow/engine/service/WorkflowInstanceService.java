package com.nitorcreations.nflow.engine.service;

import static org.springframework.util.StringUtils.isEmpty;

import java.util.Collection;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import com.nitorcreations.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

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
  @Inject
  private WorkflowInstancePreProcessor workflowInstancePreProcessor;

  public WorkflowInstanceService() {
  }

  public WorkflowInstanceService(WorkflowDefinitionService workflowDefinitionService,
                                 WorkflowInstanceDao workflowInstanceDao,
                                 WorkflowInstancePreProcessor workflowInstancePreProcessor) {
    this.workflowDefinitionService = workflowDefinitionService;
    this.workflowInstanceDao = workflowInstanceDao;
    this.workflowInstancePreProcessor = workflowInstancePreProcessor;
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
    WorkflowInstance processedInstance = workflowInstancePreProcessor.process(instance);
    int id = workflowInstanceDao.insertWorkflowInstance(processedInstance);
    if (id == -1 && !isEmpty(instance.externalId)) {
      QueryWorkflowInstances query = new QueryWorkflowInstances.Builder().addTypes(instance.type).setExternalId(instance.externalId).build();
      id = workflowInstanceDao.queryWorkflowInstances(query).get(0).id;
    }
    return id;
  }

  /**
   * Update the workflow instance in the database if it is currently not running, and insert the workflow instance action.
   * If the state of the instance is not null, the status of the instance is updated based on the new state.
   * If the state of the instance is null, neither state nor status are updated.
   * @param instance The instance to be updated.
   * @param action The action to be inserted. Can be null.
   * @return True if the update was successful, false otherwise.
   */
  @Transactional
  public boolean updateWorkflowInstance(WorkflowInstance instance, WorkflowInstanceAction action) {
    Assert.notNull(instance, "Workflow instance can not be null");
    Assert.notNull(action, "Workflow instance action can not be null");
    WorkflowInstance.Builder builder = new WorkflowInstance.Builder(instance);
    if (instance.state == null) {
      builder.setStatus(null);
    } else {
      String type = workflowInstanceDao.getWorkflowInstance(instance.id).type;
      AbstractWorkflowDefinition<?> definition = workflowDefinitionService.getWorkflowDefinition(type);
      builder.setStatus(definition.getState(instance.state).getType().getStatus(instance.nextActivation));
    }
    WorkflowInstance updatedInstance = builder.build();
    boolean updated = workflowInstanceDao.updateNotRunningWorkflowInstance(updatedInstance);
    if (updated) {
      String currentState = workflowInstanceDao.getWorkflowInstanceState(updatedInstance.id);
      WorkflowInstanceAction updatedAction = new WorkflowInstanceAction.Builder(action).setState(currentState).build();
      workflowInstanceDao.insertWorkflowInstanceAction(updatedInstance, updatedAction);
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
