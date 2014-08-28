package com.nitorcreations.nflow.engine.service;

import static org.springframework.util.StringUtils.isEmpty;

import java.util.Collection;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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

  public WorkflowInstance getWorkflowInstance(int id) {
    return workflowInstanceDao.getWorkflowInstance(id);
  }

  @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", justification = "getInitialState().toString() has no cast")
  @Transactional
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
    int id = workflowInstanceDao.insertWorkflowInstance(builder.build());
    if (id == -1 && !isEmpty(instance.externalId)) {
      QueryWorkflowInstances query = new QueryWorkflowInstances.Builder().addTypes(def.getType()).setExternalId(instance.externalId).build();
      id = workflowInstanceDao.queryWorkflowInstances(query).get(0).id;
    }
    return id;
  }

  @Transactional
  public void updateWorkflowInstance(WorkflowInstance instance, WorkflowInstanceAction action) {
    workflowInstanceDao.updateWorkflowInstance(instance);
    if (action != null) {
      workflowInstanceDao.insertWorkflowInstanceAction(instance, action);
    }
  }

  @Transactional
  public boolean wakeupWorkflowInstance(long id, String... expectedStates) {
    return workflowInstanceDao.wakeupWorkflowInstanceIfNotExecuting(id, expectedStates);
  }

  public Collection<WorkflowInstance> listWorkflowInstances(QueryWorkflowInstances query) {
    return workflowInstanceDao.queryWorkflowInstances(query);
  }

}
