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

  private final Map<String, AbstractWorkflowDefinition<? extends WorkflowState>> workflowDefinitions = new LinkedHashMap<>();
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
   * @param type Workflow definition type.
   * @return The workflow definition or null if not found.
   */
  public AbstractWorkflowDefinition<?> getWorkflowDefinition(String type) {
    return workflowDefinitions.get(type);
  }

  /**
   * Return all managed workflow definitions.
   * @return List of workflow definitions.
   */
  public List<AbstractWorkflowDefinition<? extends WorkflowState>> getWorkflowDefinitions() {
    return new ArrayList<>(workflowDefinitions.values());
  }

  /**
   * Add workflow definitions from the nflowNonSpringWorkflowsListing resource and persist
   * all loaded workflow definitions.
   */
  public void postProcessWorkflowDefinitions() {
    if (persistWorkflowDefinitions) {
      workflowDefinitions.values().forEach(workflowDefinitionDao::storeWorkflowDefinition);
    }
  }

  public void addWorkflowDefinition(AbstractWorkflowDefinition<? extends WorkflowState> wd) {
    AbstractWorkflowDefinition<? extends WorkflowState> conflict = workflowDefinitions.put(wd.getType(), wd);
    if (conflict != null) {
      throw new IllegalStateException("Both " + wd.getClass().getName() + " and " + conflict.getClass().getName() +
          " define same workflow type: " + wd.getType());
    }
    if (autoInit && persistWorkflowDefinitions) {
      workflowDefinitionDao.storeWorkflowDefinition(wd);
    }
    logger.info("Added workflow type: {} ({})",  wd.getType(), wd.getClass().getName());
  }
}
