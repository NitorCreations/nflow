package com.nitorcreations.nflow.engine.internal.executor;

import static java.util.Collections.synchronizedMap;
import static org.joda.time.DateTime.now;
import static org.joda.time.Minutes.minutesBetween;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao;
import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import com.nitorcreations.nflow.engine.listener.WorkflowExecutorListener;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;

@Component
public class WorkflowStateProcessorFactory {
  private final WorkflowDefinitionService workflowDefinitions;
  private final WorkflowInstanceService workflowInstances;
  private final ObjectStringMapper objectMapper;
  private final WorkflowInstanceDao workflowInstanceDao;
  private final WorkflowInstancePreProcessor workflowInstancePreProcessor;
  private final Environment env;
  @Autowired(required = false)
  protected WorkflowExecutorListener[] listeners = new WorkflowExecutorListener[0];
  final Map<Integer, DateTime> processingInstances = synchronizedMap(new HashMap<Integer, DateTime>());
  private static final Logger logger = getLogger(WorkflowStateProcessorFactory.class);

  @Inject
  public WorkflowStateProcessorFactory(WorkflowDefinitionService workflowDefinitions, WorkflowInstanceService workflowInstances,
      ObjectStringMapper objectMapper, WorkflowInstanceDao workflowInstanceDao,
      WorkflowInstancePreProcessor workflowInstancePreProcessor, Environment env) {
    this.workflowDefinitions = workflowDefinitions;
    this.workflowInstances = workflowInstances;
    this.objectMapper = objectMapper;
    this.workflowInstanceDao = workflowInstanceDao;
    this.workflowInstancePreProcessor = workflowInstancePreProcessor;
    this.env = env;
  }

  public WorkflowStateProcessor createProcessor(int instanceId) {
    return new WorkflowStateProcessor(instanceId, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
        workflowInstancePreProcessor, env, processingInstances, listeners);
  }

  public int getPotentiallyStuckProcessors() {
    DateTime now = now();
    int potentiallyStuck = 0;
    synchronized (processingInstances) {
      for (Entry<Integer, DateTime> entry : processingInstances.entrySet()) {
        int processingTimeMinutes = minutesBetween(entry.getValue(), now).getMinutes();
        if (processingTimeMinutes > 5) {
          potentiallyStuck++;
          logger.warn("Workflow instance {} has been processed for {} minutes, it may be stuck.", entry.getKey(),
              processingTimeMinutes);
        }
      }
    }
    return potentiallyStuck;
  }

}
