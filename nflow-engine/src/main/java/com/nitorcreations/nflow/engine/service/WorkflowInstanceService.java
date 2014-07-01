package com.nitorcreations.nflow.engine.service;

import static org.springframework.util.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

  private final WorkflowDefinitionService workflowDefinitionService;
  private final WorkflowInstanceDao workflowInstanceDao;

  @Inject
  public WorkflowInstanceService(WorkflowDefinitionService workflowDefinitionService, WorkflowInstanceDao workflowInstanceDao) {
    this.workflowDefinitionService = workflowDefinitionService;
    this.workflowInstanceDao = workflowInstanceDao;
  }

  public WorkflowInstance getWorkflowInstance(int id) {
    return workflowInstanceDao.getWorkflowInstance(id);
  }

  @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", justification = "cast is safe")
  @Transactional
  public int insertWorkflowInstance(WorkflowInstance instance) {
    WorkflowDefinition<?> def = workflowDefinitionService.getWorkflowDefinition(instance.type);
    if (def == null) {
      throw new RuntimeException("No workflow definition found for type [" + instance.type + "]");
    }
    WorkflowInstance.Builder builder = new WorkflowInstance.Builder(instance).setState(def.getInitialState().toString());
    if (isEmpty(instance.externalId)) {
      builder.setExternalId(UUID.randomUUID().toString());
    }
    return workflowInstanceDao.insertWorkflowInstance(builder.build());
  }

  @Transactional
  public void updateWorkflowInstance(WorkflowInstance instance, WorkflowInstanceAction action) {
    workflowInstanceDao.updateWorkflowInstance(instance);
    if (action != null) {
      workflowInstanceDao.insertWorkflowInstanceAction(instance, action);
    }
  }

  @Transactional
  public List<Integer> pollNextWorkflowInstanceIds(int batchSize) {
    if (batchSize > 0) {
      return workflowInstanceDao.pollNextWorkflowInstanceIds(batchSize);
    }
    return new ArrayList<>();
  }

  public Collection<WorkflowInstance> listWorkflowInstances(QueryWorkflowInstances query) {
    return workflowInstanceDao.queryWorkflowInstances(query);
  }

}
