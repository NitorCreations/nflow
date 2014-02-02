package com.nitorcreations.nflow.engine.service;

import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nitorcreations.nflow.engine.dao.RepositoryDao;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;

@Component
public class RepositoryService {

  private static final Logger logger = getLogger(RepositoryService.class);
  
  private final RepositoryDao repositoryDao;
  private Map<String, WorkflowDefinition<? extends WorkflowState>> workflowDefitions = new LinkedHashMap<>();
  
  @Inject
  public RepositoryService(RepositoryDao repositoryDao) throws Exception {
    this.repositoryDao = repositoryDao;
    initWorkflowDefinitions();
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
    instance = new WorkflowInstance.Builder(instance)   
      .setState(def.getInitialState().toString())
      .setCreated(now)
      .setModified(now)
      .build();
    return repositoryDao.insertWorkflowInstance(instance);
  }
  
  @Transactional
  public void updateWorkflowInstance(WorkflowInstance instance, boolean saveAction) {    
    WorkflowInstance saved = new WorkflowInstance.Builder(instance)
      .setModified(now())
      .build();
    repositoryDao.updateWorkflowInstance(instance);
    if (saveAction) {
      repositoryDao.insertWorkflowInstanceAction(saved);
    }
  }

  @Transactional
  public List<Integer> pollNextWorkflowInstanceIds(
      int batchSize) {
    if (batchSize > 0) {
      return repositoryDao.pollNextWorkflowInstanceIds(batchSize);
    }
    return new ArrayList<>();
  }
  
  public WorkflowDefinition<?> getWorkflowDefinition(String type) {
    return workflowDefitions.get(type);
  }
  
  public List<WorkflowDefinition<? extends WorkflowState>> getWorkflowDefinitions() {
    return new ArrayList<>(workflowDefitions.values());
  }
  
  private void initWorkflowDefinitions() throws Exception {
    BufferedReader br = new BufferedReader(
        new InputStreamReader(
            this.getClass().getClassLoader().getResourceAsStream("nflow-workflows.txt")));
    String row;
    while ((row = br.readLine()) != null) {
      logger.info("Instantiating " + row);
      @SuppressWarnings("unchecked")
      WorkflowDefinition<? extends WorkflowState> wd = (WorkflowDefinition<? extends WorkflowState>) Class.forName(row).newInstance();
      workflowDefitions.put(wd.getType(), wd);
    }
  }
  
}
