package io.nflow.engine.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;

/**
 * Service for managing workflow definitions.
 */
@Component
public class WorkflowDefinitionService {

  private static final Logger logger = getLogger(WorkflowDefinitionService.class);

  private volatile Map<String, AbstractWorkflowDefinition<? extends WorkflowState>> workflowDefinitions = new LinkedHashMap<>();
  private final WorkflowDefinitionDao workflowDefinitionDao;
  private final boolean autoPersistDefinitions;

  @Inject
  public WorkflowDefinitionService(WorkflowDefinitionDao workflowDefinitionDao, Environment env) {
    this.workflowDefinitionDao = workflowDefinitionDao;
    this.autoPersistDefinitions = env.getRequiredProperty("nflow.definition.autopersist", Boolean.class);
  }

  /**
   * Return the workflow definition that matches the give workflow type name.
   *
   * @param type
   *          Workflow definition type.
   * @return The workflow definition or null if not found.
   */
  public AbstractWorkflowDefinition<?> getWorkflowDefinition(String type) {
    return workflowDefinitions.get(type);
  }

  /**
   * Return all managed workflow definitions.
   *
   * @return List of workflow definitions.
   */
  public List<AbstractWorkflowDefinition<? extends WorkflowState>> getWorkflowDefinitions() {
    return new ArrayList<>(workflowDefinitions.values());
  }

  /**
   * Persist all loaded workflow definitions to database if nflow.definition.autopersist is false. If nflow.definition.autopersist
   * is true, definitions are persisted automatically when they are added to managed definitions.
   */
  public void persistWorkflowDefinitions() {
    if (!autoPersistDefinitions) {
      workflowDefinitions.values().forEach(workflowDefinitionDao::storeWorkflowDefinition);
    }
  }

  /**
   * Add given workflow definition to managed definitions. Persist given definition to database if nflow.definition.autopersist is
   * true. If nflow.definition.autopersist is false, call persistWorkflowDefinitions manually if needed to persist the
   * definitions.
   *
   * @param wd
   *          The workflow definition to be added.
   * @throws IllegalStateException
   *           When a definition with the same type has already been added.
   */
  public void addWorkflowDefinition(AbstractWorkflowDefinition<? extends WorkflowState> wd) {
    synchronized (this) {
      Map<String, AbstractWorkflowDefinition<? extends WorkflowState>> newDefinitions = new LinkedHashMap<>(workflowDefinitions);
      AbstractWorkflowDefinition<? extends WorkflowState> conflict = newDefinitions.put(wd.getType(), wd);
      if (conflict != null) {
        throw new IllegalStateException("Both " + wd.getClass().getName() + " and " + conflict.getClass().getName()
            + " define same workflow type: " + wd.getType());
      }
      workflowDefinitions = newDefinitions;
    }
    if (autoPersistDefinitions) {
      workflowDefinitionDao.storeWorkflowDefinition(wd);
    }
    logger.info("Added workflow type: {} ({})", wd.getType(), wd.getClass().getName());
  }
}
