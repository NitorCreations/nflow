package io.nflow.tests.demo;

import static io.nflow.engine.internal.config.Profiles.JMX;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static io.nflow.tests.demo.SpringApplicationContext.applicationContext;
import static org.joda.time.DateTime.now;

import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.jetty.StartNflow;
import io.nflow.metrics.NflowMetricsContext;

public class DemoServer {

  public static void main(String[] args) throws Exception {
    new StartNflow().registerSpringContext(NflowMetricsContext.class, SpringApplicationContext.class, DemoWorkflow.class)
        .startJetty(7500, "local", JMX);
    insertDemoWorkflows();
  }

  private static void insertDemoWorkflows() {
    WorkflowInstanceService workflowInstanceService = applicationContext.getBean(WorkflowInstanceService.class);
    WorkflowInstance instance = new WorkflowInstance.Builder().setType("demo").setState("begin").build();
    int id = workflowInstanceService.insertWorkflowInstance(instance);
    instance = workflowInstanceService.getWorkflowInstance(id);
    WorkflowInstanceAction action = new WorkflowInstanceAction.Builder(instance).setType(externalChange).setExecutionEnd(now())
        .build();
    workflowInstanceService.updateWorkflowInstance(instance, action);
    QueryWorkflowInstances query = new QueryWorkflowInstances.Builder().addIds(id).setIncludeActions(true).build();
    instance = workflowInstanceService.listWorkflowInstances(query).iterator().next();
    int actionId = instance.actions.get(0).id;
    WorkflowInstance child = new WorkflowInstance.Builder().setType("demo").setState("begin").setParentActionId(actionId)
        .setParentWorkflowId(id).build();
    workflowInstanceService.insertWorkflowInstance(child);
  }
}
