package com.nitorcreations.nflow.engine.service;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

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

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
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
  private final ApplicationContext appCtx;
  private final Map<String, WorkflowDefinition<? extends WorkflowState>> workflowDefitions = new LinkedHashMap<>();

  @Inject
  public RepositoryService(RepositoryDao repositoryDao, ApplicationContext appCtx) throws Exception {
    this.repositoryDao = repositoryDao;
    this.appCtx = appCtx;
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

  @SuppressWarnings("unchecked")
  @PostConstruct
  public void initWorkflowDefinitions() throws Exception {
    BufferedReader br = new BufferedReader(
        new InputStreamReader(
            this.getClass().getClassLoader().getResourceAsStream("nflow-workflows.txt")));
    String row;
    while ((row = br.readLine()) != null) {
      logger.info("Preparing workflow " + row);
      WorkflowDefinition<? extends WorkflowState> wd;
      Class<WorkflowDefinition<? extends WorkflowState>> clazz = (Class<WorkflowDefinition<? extends WorkflowState>>) Class.forName(row);
      try {
        wd = appCtx.getBean(clazz);
        logger.info("Found " + row + " Spring bean");
      } catch(NoSuchBeanDefinitionException nex) {
        logger.info("Not found " + row + " Spring bean, instantiating as a class");
        wd = (WorkflowDefinition<? extends WorkflowState>) Class.forName(row).newInstance();
      }
      workflowDefitions.put(wd.getType(), wd);
    }
  }

}
