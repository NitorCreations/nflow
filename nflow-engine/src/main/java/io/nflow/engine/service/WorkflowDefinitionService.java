package io.nflow.engine.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.AbstractResource;
import org.springframework.stereotype.Component;

import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;

/**
 * Service for managing workflow definitions.
 */
@Component
public class WorkflowDefinitionService {

  private static final Logger logger = getLogger(WorkflowDefinitionService.class);

  private AbstractResource nonSpringWorkflowsListing;
  private final Map<String, AbstractWorkflowDefinition<? extends WorkflowState>> workflowDefinitions = new LinkedHashMap<>();
  private final WorkflowDefinitionDao workflowDefinitionDao;
  private final boolean persistWorkflowDefinitions;

  @Inject
  public WorkflowDefinitionService(WorkflowDefinitionDao workflowDefinitionDao, Environment env) {
    this.workflowDefinitionDao = workflowDefinitionDao;
    this.persistWorkflowDefinitions = env.getRequiredProperty("nflow.definition.persist", Boolean.class);
  }

  /**
   * Add given workflow definitions to the managed definitions.
   * @param workflowDefinitions The workflow definitions to be added.
   */
  @Autowired(required = false)
  public void setWorkflowDefinitions(Collection<AbstractWorkflowDefinition<? extends WorkflowState>> workflowDefinitions) {
    for (AbstractWorkflowDefinition<? extends WorkflowState> wd : workflowDefinitions) {
      addWorkflowDefinition(wd);
    }
  }

  @Autowired(required = false)
  public void setWorkflowDefinitions(@NFlow AbstractResource nflowNonSpringWorkflowsListing) {
    this.nonSpringWorkflowsListing = nflowNonSpringWorkflowsListing;
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
   * @throws IOException when workflow definitions can not be read from the resource.
   * @throws ReflectiveOperationException when the workflow definition can not be instantiated.
   */
  public void postProcessWorkflowDefinitions() throws IOException, ReflectiveOperationException {
    if (nonSpringWorkflowsListing == null) {
      logger.info("No non-Spring workflow definitions");
    } else {
      initNonSpringWorkflowDefinitions();
    }
    if (persistWorkflowDefinitions) {
      for (AbstractWorkflowDefinition<?> definition : workflowDefinitions.values()) {
        workflowDefinitionDao.storeWorkflowDefinition(definition);
      }
    }
  }

  private void initNonSpringWorkflowDefinitions() throws IOException, ReflectiveOperationException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(nonSpringWorkflowsListing.getInputStream(), UTF_8))) {
      String row;
      while ((row = br.readLine()) != null) {
        logger.info("Preparing workflow {}", row);
        @SuppressWarnings("unchecked")
        Class<AbstractWorkflowDefinition<? extends WorkflowState>> clazz = (Class<AbstractWorkflowDefinition<? extends WorkflowState>>) Class.forName(row);
        addWorkflowDefinition(clazz.getDeclaredConstructor().newInstance());
      }
    }
  }

  public void addWorkflowDefinition(AbstractWorkflowDefinition<? extends WorkflowState> wd) {
    AbstractWorkflowDefinition<? extends WorkflowState> conflict = workflowDefinitions.put(wd.getType(), wd);
    if (conflict != null) {
      throw new IllegalStateException("Both " + wd.getClass().getName() + " and " + conflict.getClass().getName() +
          " define same workflow type: " + wd.getType());
    }
    logger.info("Added workflow type: {} ({})",  wd.getType(), wd.getClass().getName());
  }
}
