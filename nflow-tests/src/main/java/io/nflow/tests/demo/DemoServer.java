package io.nflow.tests.demo;

import static io.nflow.engine.config.Profiles.JMX;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static io.nflow.tests.demo.SpringApplicationContext.applicationContext;
import static io.nflow.tests.demo.workflow.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.SlowWorkflow.SLOW_WORKFLOW_TYPE;
import static java.util.Collections.emptySet;
import static org.joda.time.DateTime.now;

import java.util.EnumSet;
import java.util.Optional;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.jetty.StartNflow;
import io.nflow.metrics.NflowMetricsContext;
import io.nflow.tests.demo.workflow.DemoWorkflow;

public class DemoServer {

  public static void main(String[] args) throws Exception {
    new StartNflow()
        .registerSpringContext(NflowMetricsContext.class, SpringApplicationContext.class, DemoServerWorkflowsConfiguration.class)
        .startJetty(7500, "local", JMX);
    insertDemoWorkflows();
  }

  @Configuration
  @ComponentScan("io.nflow.tests.demo.workflow")
  static class DemoServerWorkflowsConfiguration {
  }

  private static void insertDemoWorkflows() {
    WorkflowInstanceService workflowInstanceService = applicationContext.getBean(WorkflowInstanceService.class);
    WorkflowInstance instance = new WorkflowInstance.Builder().setType(DEMO_WORKFLOW_TYPE)
        .setState(DemoWorkflow.State.begin.name()).build();
    int id = workflowInstanceService.insertWorkflowInstance(instance);
    instance = workflowInstanceService.getWorkflowInstance(id, emptySet(), null);
    WorkflowInstanceAction action = new WorkflowInstanceAction.Builder(instance).setType(externalChange).setExecutionEnd(now())
        .build();
    workflowInstanceService.updateWorkflowInstance(instance, action);
    instance = workflowInstanceService.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.ACTIONS), 1L);
    int actionId = instance.actions.get(0).id;
    WorkflowInstance child = new WorkflowInstance.Builder().setType(DEMO_WORKFLOW_TYPE).setState(DemoWorkflow.State.begin.name())
        .setParentActionId(actionId).setParentWorkflowId(id).build();
    workflowInstanceService.insertWorkflowInstance(child);
    instance = new WorkflowInstance.Builder().setType(SLOW_WORKFLOW_TYPE).setSignal(Optional.of(1)).setNextActivation(null)
        .build();
    workflowInstanceService.insertWorkflowInstance(instance);
  }
}
