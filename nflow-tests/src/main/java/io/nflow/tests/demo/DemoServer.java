package io.nflow.tests.demo;

import static io.nflow.engine.config.Profiles.JMX;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static io.nflow.tests.demo.SpringApplicationContext.applicationContext;
import static io.nflow.tests.demo.workflow.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.ForeverWaitingWorkflow.FOREVER_WAITING_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.SlowWorkflow.SLOW_WORKFLOW_TYPE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.joda.time.DateTime.now;

import java.util.EnumSet;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.definition.BulkWorkflow;
import io.nflow.engine.workflow.executor.WorkflowLogContextListener;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.jetty.StartNflow;
import io.nflow.metrics.NflowMetricsContext;
import io.nflow.tests.demo.workflow.DemoBulkWorkflow;
import io.nflow.tests.demo.workflow.DemoWorkflow;
import io.nflow.tests.demo.workflow.MaintenanceCronWorkflow;

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
    @Bean
    public WorkflowLogContextListener logContextListener() {
      return new WorkflowLogContextListener("context");
    }
  }

  private static void insertDemoWorkflows() {
    WorkflowInstanceService workflowInstanceService = applicationContext.getBean(WorkflowInstanceService.class);
    WorkflowInstanceFactory workflowInstanceFactory = applicationContext.getBean(WorkflowInstanceFactory.class);
    WorkflowInstance instance = new WorkflowInstance.Builder().setType(DEMO_WORKFLOW_TYPE)
        .setState(DemoWorkflow.State.begin.name()).build();
    long id = workflowInstanceService.insertWorkflowInstance(instance);
    instance = workflowInstanceService.getWorkflowInstance(id, emptySet(), null);
    WorkflowInstanceAction action = new WorkflowInstanceAction.Builder(instance).setType(externalChange).setExecutionEnd(now())
        .build();
    workflowInstanceService.updateWorkflowInstance(instance, action);
    instance = workflowInstanceService.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.ACTIONS), 1L);
    long actionId = instance.actions.get(0).id;
    WorkflowInstance child = new WorkflowInstance.Builder().setType(DEMO_WORKFLOW_TYPE).setState(DemoWorkflow.State.begin.name())
        .setParentActionId(actionId).setParentWorkflowId(id).build();
    workflowInstanceService.insertWorkflowInstance(child);
    instance = new WorkflowInstance.Builder().setType(SLOW_WORKFLOW_TYPE).setSignal(Optional.of(1)).setNextActivation(null)
        .build();
    workflowInstanceService.insertWorkflowInstance(instance);
    // insert demo bulk workflow with couple of children
    instance = workflowInstanceFactory.newWorkflowInstanceBuilder() //
        .setType(DemoBulkWorkflow.DEMO_BULK_WORKFLOW_TYPE) //
        .setState(BulkWorkflow.State.splitWork.name()) //
        .putStateVariable(BulkWorkflow.VAR_CONCURRENCY, 2) //
        .putStateVariable(BulkWorkflow.VAR_CHILD_DATA, asList(1, 2, 3, 4, 5)) //
        .build();
    workflowInstanceService.insertWorkflowInstance(instance);
    instance = new WorkflowInstance.Builder().setType(FOREVER_WAITING_WORKFLOW_TYPE).build();
    workflowInstanceService.insertWorkflowInstance(instance);
    instance = new WorkflowInstance.Builder().setType(MaintenanceCronWorkflow.TYPE).build();
    workflowInstanceService.insertWorkflowInstance(instance);
  }
}
