package io.nflow.engine.service;

import static java.lang.Long.MAX_VALUE;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.internal.workflow.StoredWorkflowDefinition;
import io.nflow.engine.internal.workflow.StoredWorkflowDefinitionWrapper;
import io.nflow.engine.workflow.definition.WorkflowDefinition;

/**
 * Service for managing workflow definitions.
 */
@Component
@Singleton
public class WorkflowDefinitionService {

  private static final Logger logger = getLogger(WorkflowDefinitionService.class);

  private final Map<String, WorkflowDefinition> workflowDefinitions = synchronizedMap(new LinkedHashMap<>());
  private List<WorkflowDefinition> workflowDefinitionValues = emptyList();
  private final WorkflowDefinitionDao workflowDefinitionDao;
  private final boolean persistWorkflowDefinitions;
  private final boolean autoInit;
  private final long storedDefinitionCheckInterval;
  /**
   * The next time the stored definitions from database are checked for changes.
   */
  private long nextCheckOfStoredDefinitions;

  @Inject
  public WorkflowDefinitionService(WorkflowDefinitionDao workflowDefinitionDao, Environment env) {
    this.workflowDefinitionDao = workflowDefinitionDao;
    this.persistWorkflowDefinitions = env.getRequiredProperty("nflow.definition.persist", Boolean.class);
    this.autoInit = env.getRequiredProperty("nflow.autoinit", Boolean.class);
    // TODO: config property name? also in CHANGES.md
    this.storedDefinitionCheckInterval = SECONDS
        .toMillis(env.getRequiredProperty("nflow.definition.loadMissingFromDatabaseSeconds.interval.seconds", Integer.class));
    this.nextCheckOfStoredDefinitions = storedDefinitionCheckInterval > 0 ? 0 : MAX_VALUE;
  }

  /**
   * Return the workflow definition that matches the give workflow type name.
   *
   * @param type
   *          Workflow definition type.
   * @return The workflow definition or null if not found.
   */
  public WorkflowDefinition getWorkflowDefinition(String type) {
    WorkflowDefinition definition = workflowDefinitions.get(type);
    if (definition instanceof StoredWorkflowDefinitionWrapper && getWorkflowDefinitionRefreshTime() > 0) {
      definition = null;
    }
    if (definition == null && refreshStoredDefinitions()) {
      definition = workflowDefinitions.get(type);
    }
    return definition;
  }

  /**
   * Return all managed workflow definitions.
   *
   * @return List of workflow definitions.
   */
  public List<WorkflowDefinition> getWorkflowDefinitions() {
    refreshStoredDefinitions();
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
  public void addWorkflowDefinition(WorkflowDefinition wd) {
    WorkflowDefinition conflict = workflowDefinitions.put(wd.getType(), wd);
    if (conflict != null) {
      throw new IllegalStateException("Both " + wd.getClass().getName() + " and " + conflict.getClass().getName()
          + " define same workflow type: " + wd.getType());
    }
    if (autoInit && persistWorkflowDefinitions) {
      workflowDefinitionDao.storeWorkflowDefinition(wd);
    }
    setWorkflowDefinitions(workflowDefinitions.values());
    logger.info("Added workflow type: {} ({})", wd.getType(), wd.getClass().getName());
  }

  private void setWorkflowDefinitions(Collection<WorkflowDefinition> newDefinitions) {
    synchronized (workflowDefinitions) {
      workflowDefinitionValues = unmodifiableList(new ArrayList<>(newDefinitions));
    }
  }

  @SuppressFBWarnings(value = "NOS_NON_OWNED_SYNCHRONIZATION",
      justification = "synchronize(this) is valid and needed to match the below synchronized refreshStoredDefinitions() method")
  private long getWorkflowDefinitionRefreshTime() {
    if (storedDefinitionCheckInterval <= 0) {
      return -1;
    }
    long now = currentTimeMillis();
    synchronized (this) {
      if (nextCheckOfStoredDefinitions <= now) {
        return now;
      }
    }
    return -1;
  }

  private synchronized boolean refreshStoredDefinitions() {
    long now = getWorkflowDefinitionRefreshTime();
    if (now <= -1) {
      return false;
    }
    nextCheckOfStoredDefinitions = now + storedDefinitionCheckInterval;
    boolean changed = false;
    for (StoredWorkflowDefinition def : workflowDefinitionDao.queryStoredWorkflowDefinitions(emptyList())) {
      WorkflowDefinition current = workflowDefinitions.get(def.type);
      if (current == null || current instanceof StoredWorkflowDefinitionWrapper) {
        StoredWorkflowDefinitionWrapper wrapper = new StoredWorkflowDefinitionWrapper(def);
        workflowDefinitions.put(def.type, wrapper);
        changed = true;
      }
    }
    if (changed) {
      setWorkflowDefinitions(workflowDefinitions.values());
    }
    return changed;
  }
}
