package com.nitorcreations.nflow.engine.service;

import static com.nitorcreations.nflow.engine.internal.config.EngineConfiguration.NFLOW_NON_SPRING_WORKFLOWS_LISTING;
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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.AbstractResource;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecutionStatistics;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;

/**
 * Service for managing workflow definitions.
 */
@Component
public class WorkflowDefinitionService {

  private static final Logger logger = getLogger(WorkflowDefinitionService.class);

  private final AbstractResource nonSpringWorkflowsListing;
  private final Map<String, WorkflowDefinition<? extends WorkflowState>> workflowDefitions = new LinkedHashMap<>();
  private final WorkflowInstanceDao workflowInstanceDao;

  @Inject
  public WorkflowDefinitionService(@Named(NFLOW_NON_SPRING_WORKFLOWS_LISTING) AbstractResource nonSpringWorkflowsListing, WorkflowInstanceDao workflowInstanceDao) {
    this.nonSpringWorkflowsListing = nonSpringWorkflowsListing;
    this.workflowInstanceDao = workflowInstanceDao;
  }

  /**
   * Add given workflow definitions to the managed definitions.
   * @param workflowDefinitions The workflow definitions to be added.
   */
  @Autowired(required=false)
  public void setWorkflowDefinitions(Collection<WorkflowDefinition<? extends WorkflowState>> workflowDefinitions) {
    for (WorkflowDefinition<? extends WorkflowState> wd : workflowDefinitions) {
      addWorkflowDefinition(wd);
    }
  }

  /**
   * Return the workflow definition that matches the give workflow type name.
   * @param type Workflow definition type.
   * @return The workflow definition or null if not found.
   */
  public WorkflowDefinition<?> getWorkflowDefinition(String type) {
    return workflowDefitions.get(type);
  }

  /**
   * Return all managed workflow definitions.
   * @return List of workflow definitions.
   */
  public List<WorkflowDefinition<? extends WorkflowState>> getWorkflowDefinitions() {
    return new ArrayList<>(workflowDefitions.values());
  }

  /**
   * Add workflow definitions from the nflowNonSpringWorkflowsListing resource.
   * @throws IOException when workflow definitions can not be read from the resource.
   * @throws ReflectiveOperationException when the workflow definition can not be instantiated.
   */
  @PostConstruct
  public void initNonSpringWorkflowDefinitions() throws IOException, ReflectiveOperationException {
    if (nonSpringWorkflowsListing == null) {
      logger.info("No non-Spring workflow definitions");
      return;
    }
    try (BufferedReader br = new BufferedReader(new InputStreamReader(nonSpringWorkflowsListing.getInputStream(), UTF_8))) {
      String row;
      while ((row = br.readLine()) != null) {
        logger.info("Preparing workflow {}", row);
        @SuppressWarnings("unchecked")
        Class<WorkflowDefinition<? extends WorkflowState>> clazz = (Class<WorkflowDefinition<? extends WorkflowState>>) Class.forName(row);
        addWorkflowDefinition(clazz.newInstance());
      }
    }
  }

  private void addWorkflowDefinition(WorkflowDefinition<? extends WorkflowState> wd) {
    WorkflowDefinition<? extends WorkflowState> conflict = workflowDefitions.put(wd.getType(), wd);
    if (conflict != null) {
      throw new IllegalStateException("Both " + wd.getClass().getName() + " and " + conflict.getClass().getName() +
          " define same workflow type: " + wd.getType());
    }
    logger.info("Added workflow type: {} ({})",  wd.getType(), wd.getClass().getName());
  }

  /**
   * Return workflow definition statistics for a given type.
   * @param type The workflow definition type.
   * @param createdAfter If given, count only workflow instances created after this time.
   * @param createdBefore If given, count only workflow instances created before this time.
   * @param modifiedAfter If given, count only workflow instances modified after this time.
   * @param modifiedBefore If given, count only workflow instances modified after this time.
   * @return The statistics per workflow state.
   */
  public Map<String, StateExecutionStatistics> getStatistics(String type, DateTime createdAfter, DateTime createdBefore,
      DateTime modifiedAfter, DateTime modifiedBefore) {
    return workflowInstanceDao.getStateExecutionStatistics(type, createdAfter, createdBefore, modifiedAfter, modifiedBefore);
  }
}
