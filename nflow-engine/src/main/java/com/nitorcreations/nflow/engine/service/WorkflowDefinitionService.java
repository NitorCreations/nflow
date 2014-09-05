package com.nitorcreations.nflow.engine.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.AbstractResource;
import org.springframework.stereotype.Component;

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

  @Inject
  public WorkflowDefinitionService(@Named("nflowNonSpringWorkflowsListing") AbstractResource nonSpringWorkflowsListing) {
    this.nonSpringWorkflowsListing = nonSpringWorkflowsListing;
  }

  /**
   * Add given workflow definitions to the managed definitions.
   */
  @Autowired(required=false)
  public void setWorkflowDefinitions(Collection<WorkflowDefinition<? extends WorkflowState>> workflowDefinitions) {
    for (WorkflowDefinition<? extends WorkflowState> wd : workflowDefinitions) {
      addWorkflowDefinition(wd);
    }
  }

  /**
   * Return the workflow definition that matches the give workflow type name.
   * @return The workflow definition or null if not found.
   */
  public WorkflowDefinition<?> getWorkflowDefinition(String type) {
    return workflowDefitions.get(type);
  }

  /**
   * Return all managed workflow definitions.
   * @return
   */
  public List<WorkflowDefinition<? extends WorkflowState>> getWorkflowDefinitions() {
    return new ArrayList<>(workflowDefitions.values());
  }

  /**
   * Add workflow definitions from the nflowNonSpringWorkflowsListing resource.
   */
  @PostConstruct
  public void initNonSpringWorkflowDefinitions() throws Exception {
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
}
