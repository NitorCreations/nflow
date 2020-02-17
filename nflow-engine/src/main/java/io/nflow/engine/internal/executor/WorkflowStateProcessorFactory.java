package io.nflow.engine.internal.executor;

import static org.joda.time.DateTimeUtils.currentTimeMillis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import io.nflow.engine.listener.WorkflowExecutorListener;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.service.WorkflowInstanceService;

@Component
public class WorkflowStateProcessorFactory {
  private final WorkflowDefinitionService workflowDefinitions;
  private final WorkflowInstanceService workflowInstances;
  private final ObjectStringMapper objectMapper;
  private final WorkflowInstanceDao workflowInstanceDao;
  private final MaintenanceDao maintenanceDao;
  private final WorkflowInstancePreProcessor workflowInstancePreProcessor;
  private final Environment env;
  @Autowired(required = false)
  protected WorkflowExecutorListener[] listeners = new WorkflowExecutorListener[0];
  final Map<Long, WorkflowStateProcessor> processingInstances = new ConcurrentHashMap<>();
  private final int stuckThreadThresholdSeconds;

  @Inject
  public WorkflowStateProcessorFactory(WorkflowDefinitionService workflowDefinitions, WorkflowInstanceService workflowInstances,
      ObjectStringMapper objectMapper, WorkflowInstanceDao workflowInstanceDao, MaintenanceDao maintenanceDao,
      WorkflowInstancePreProcessor workflowInstancePreProcessor, Environment env) {
    this.workflowDefinitions = workflowDefinitions;
    this.workflowInstances = workflowInstances;
    this.objectMapper = objectMapper;
    this.workflowInstanceDao = workflowInstanceDao;
    this.maintenanceDao = maintenanceDao;
    this.workflowInstancePreProcessor = workflowInstancePreProcessor;
    this.stuckThreadThresholdSeconds = env.getRequiredProperty("nflow.executor.stuckThreadThreshold.seconds", Integer.class);
    this.env = env;
  }

  public WorkflowStateProcessor createProcessor(long instanceId) {
    return new WorkflowStateProcessor(instanceId, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
        maintenanceDao, workflowInstancePreProcessor, env, processingInstances, listeners);
  }

  public int getPotentiallyStuckProcessors() {
    long currentTimeSeconds = currentTimeMillis() / 1000;
    int potentiallyStuck = 0;
    for (WorkflowStateProcessor processor : processingInstances.values()) {
      long processingTimeSeconds = currentTimeSeconds - processor.getStartTimeSeconds();
      if (processingTimeSeconds > stuckThreadThresholdSeconds) {
        potentiallyStuck++;
        processor.logPotentiallyStuck(processingTimeSeconds);
      }
    }
    return potentiallyStuck;
  }

}
