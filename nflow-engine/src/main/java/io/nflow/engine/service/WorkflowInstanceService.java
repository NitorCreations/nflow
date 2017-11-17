package io.nflow.engine.service;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;

/**
 * Service for managing workflow instances.
 */
@Component
public class WorkflowInstanceService {

  private static final Logger logger = getLogger(WorkflowInstanceService.class);

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
   * @param includes Set of properties to be loaded.
   * @param maxActions Maximum number of actions to be loaded.
   * @return The workflow instance, or null if not found.
   */
  public WorkflowInstance getWorkflowInstance(int id, Set<WorkflowInstanceInclude> includes, Long maxActions) {
    return workflowInstanceDao.getWorkflowInstance(id, includes, maxActions);
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
   * @param action The action to be inserted.
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
      String type = workflowInstanceDao.getWorkflowInstanceType(instance.id);
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
   * @param expectedStates The expected states, empty for any.
   * @return True if the instance was woken up, false otherwise.
   */
  @Transactional
  public boolean wakeupWorkflowInstance(long id, List<String> expectedStates) {
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

  /**
   * Return current signal value for given workflow instance.
   * @param workflowInstanceId Workflow instance id.
   * @return Current signal value.
   */
  public Optional<Integer> getSignal(Integer workflowInstanceId) {
    return workflowInstanceDao.getSignal(workflowInstanceId);
  }

  /**
   * Set signal value for given workflow instance.
   * @param workflowInstanceId Workflow instance id.
   * @param signal New value for the signal.
   * @param reason The reason for setting the signal.
   * @param actionType The type of workflow action that is stored to instance actions.
   * @return True when signal was set, false otherwise.
   */
  public boolean setSignal(Integer workflowInstanceId, Optional<Integer> signal, String reason, WorkflowActionType actionType) {
    signal.ifPresent(signalValue -> {
      AbstractWorkflowDefinition<?> definition = getDefinition(workflowInstanceId);
      if (!definition.getSupportedSignals().containsKey(signalValue)) {
        logger.warn("Setting unsupported signal value {} to instance {}.", signalValue, workflowInstanceId);
      }
    });
    return workflowInstanceDao.setSignal(workflowInstanceId, signal, reason, actionType);
  }

  private AbstractWorkflowDefinition<?> getDefinition(Integer workflowInstanceId) {
    return workflowDefinitionService.getWorkflowDefinition(workflowInstanceDao.getWorkflowInstanceType(workflowInstanceId));
  }

}
