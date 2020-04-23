package io.nflow.engine.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedMap;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

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
@Singleton
public class WorkflowDefinitionService {

  private static final Logger logger = getLogger(WorkflowDefinitionService.class);

  private final Map<String, AbstractWorkflowDefinition<? extends WorkflowState>> workflowDefinitions = synchronizedMap(
      new LinkedHashMap<>());
  private List<AbstractWorkflowDefinition<? extends WorkflowState>> workflowDefinitionValues = emptyList();
  private final WorkflowDefinitionDao workflowDefinitionDao;
  private final boolean persistWorkflowDefinitions;
  private final boolean autoInit;

  @Inject
  public WorkflowDefinitionService(WorkflowDefinitionDao workflowDefinitionDao, Environment env) {
    this.workflowDefinitionDao = workflowDefinitionDao;
    this.persistWorkflowDefinitions = env.getRequiredProperty("nflow.definition.persist", Boolean.class);
    this.autoInit = env.getRequiredProperty("nflow.autoinit", Boolean.class);
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
    return workflowDefinitionValues;
  }

  /**
   * Persist all loaded workflow definitions if nflow.autoinit is false and nflow.definition.persist is true. If nflow.autoinit is
   * true, definitions are persisted when they are added to managed definitions.
   */
  public void postProcessWorkflowDefinitions() {
    if (!autoInit && persistWorkflowDefinitions) {
      synchronized (workflowDefinitions) {
        workflowDefinitions.values().forEach(workflowDefinitionDao::storeWorkflowDefinition);
      }
    }
  }

  /**
   * Add given workflow definition to managed definitions. Persist given definition if nflow.autoinit and nflow.definition.persist
   * are true.
   *
   * @param wd
   *          The workflow definition to be added.
   * @throws IllegalStateException
   *           When a definition with the same type has already been added.
   */
  public void addWorkflowDefinition(AbstractWorkflowDefinition<? extends WorkflowState> wd) {
    AbstractWorkflowDefinition<? extends WorkflowState> conflict = workflowDefinitions.put(wd.getType(), wd);
    if (conflict != null) {
      throw new IllegalStateException("Both " + wd.getClass().getName() + " and " + conflict.getClass().getName()
          + " define same workflow type: " + wd.getType());
    }
    if (autoInit && persistWorkflowDefinitions) {
      workflowDefinitionDao.storeWorkflowDefinition(wd);
    }
    synchronized (workflowDefinitions) {
      workflowDefinitionValues = new ArrayList<>(workflowDefinitions.values());
    }
    logger.info("Added workflow type: {} ({})", wd.getType(), wd.getClass().getName());
  }
}
