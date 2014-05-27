package com.nitorcreations.nflow.engine.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.StringUtils.isEmpty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.AbstractResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nitorcreations.nflow.engine.dao.RepositoryDao;
import com.nitorcreations.nflow.engine.domain.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.domain.WorkflowInstanceAction;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;

@Component
public class RepositoryService {

  private static final Logger logger = getLogger(RepositoryService.class);

  private final RepositoryDao repositoryDao;
  private final AbstractResource nonSpringWorkflowsListing;
  private final Map<String, WorkflowDefinition<? extends WorkflowState>> workflowDefitions = new LinkedHashMap<>();

  @Inject
  public RepositoryService(RepositoryDao repositoryDao,
      @Named("non-spring-workflows-listing") AbstractResource nonSpringWorkflowsListing) throws Exception {
    this.repositoryDao = repositoryDao;
    this.nonSpringWorkflowsListing = nonSpringWorkflowsListing;
  }

  @Autowired(required=false)
  public void setWorkflowDefinitions(Collection<WorkflowDefinition<? extends WorkflowState>> workflowDefinitions) {
    for (WorkflowDefinition<? extends WorkflowState> wd : workflowDefinitions) {
      addWorkflowDefinition(wd);
    }
  }

  public WorkflowInstance getWorkflowInstance(int id) {
    return repositoryDao.getWorkflowInstance(id);
  }

  @Transactional
  public int insertWorkflowInstance(WorkflowInstance instance) {
    WorkflowDefinition<?> def = getWorkflowDefinition(instance.type);
    if (def == null) {
      throw new RuntimeException("No workflow definition found for type [" + instance.type + "]");
    }
    DateTime now = now();
    WorkflowInstance.Builder builder = new WorkflowInstance.Builder(instance).setState(def.getInitialState().toString())
      .setCreated(now).setModified(now);
    if (isEmpty(instance.externalId)) {
      builder.setExternalId(UUID.randomUUID().toString());
    }
    return repositoryDao.insertWorkflowInstance(builder.build());
  }

  @Transactional
  public WorkflowInstance updateWorkflowInstance(WorkflowInstance instance, WorkflowInstanceAction action) {
    WorkflowInstance saved = new WorkflowInstance.Builder(instance)
      .setModified(now())
      .build();
    repositoryDao.updateWorkflowInstance(saved);
    if (action != null) {
      repositoryDao.insertWorkflowInstanceAction(saved, action);
    }
    return saved;
  }

  @Transactional
  public List<Integer> pollNextWorkflowInstanceIds(
      int batchSize) {
    if (batchSize > 0) {
      return repositoryDao.pollNextWorkflowInstanceIds(batchSize);
    }
    return new ArrayList<>();
  }

  public Collection<WorkflowInstance> listWorkflowInstances(QueryWorkflowInstances query) {
    return repositoryDao.queryWorkflowInstances(query);
  }

  public WorkflowDefinition<?> getWorkflowDefinition(String type) {
    return workflowDefitions.get(type);
  }

  public List<WorkflowDefinition<? extends WorkflowState>> getWorkflowDefinitions() {
    return new ArrayList<>(workflowDefitions.values());
  }

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
