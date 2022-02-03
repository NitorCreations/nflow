package io.nflow.tests.demo;

import static io.nflow.engine.config.Profiles.JMX;
import static io.nflow.engine.workflow.curated.BulkWorkflow.SPLIT_WORK;
import static io.nflow.engine.workflow.curated.BulkWorkflow.VAR_CHILD_DATA;
import static io.nflow.engine.workflow.curated.BulkWorkflow.VAR_CONCURRENCY;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static io.nflow.tests.demo.SpringApplicationContext.applicationContext;
import static io.nflow.tests.demo.workflow.DemoBulkWorkflow.DEMO_BULK_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.ForeverWaitingWorkflow.FOREVER_WAITING_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.SlowWorkflow.SLOW_WORKFLOW_TYPE;
import static io.nflow.tests.demo.workflow.TestState.BEGIN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.joda.time.DateTime.now;

import java.util.EnumSet;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.executor.WorkflowLogContextListener;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.jetty.StartNflow;
import io.nflow.metrics.NflowMetricsContext;

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

  @SuppressFBWarnings(value = "OI_OPTIONAL_ISSUES_PRIMITIVE_VARIANT_PREFERRED",
      justification = "Fix would break backwards compatibility")
  private static void insertDemoWorkflows() {
    WorkflowInstanceService workflowInstanceService = applicationContext.getBean(WorkflowInstanceService.class);
    WorkflowInstanceFactory workflowInstanceFactory = applicationContext.getBean(WorkflowInstanceFactory.class);
    WorkflowInstance instance = new WorkflowInstance.Builder().setType(DEMO_WORKFLOW_TYPE).setState(BEGIN.name()).build();
    long id = workflowInstanceService.insertWorkflowInstance(instance);
    instance = workflowInstanceService.getWorkflowInstance(id, emptySet(), null);
    WorkflowInstanceAction action = new WorkflowInstanceAction.Builder(instance).setType(externalChange).setExecutionEnd(now())
        .build();
    workflowInstanceService.updateWorkflowInstance(instance, action);
    instance = workflowInstanceService.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.ACTIONS), 1L);
    long actionId = instance.actions.get(0).id;
    WorkflowInstance child = new WorkflowInstance.Builder().setType(DEMO_WORKFLOW_TYPE).setState(BEGIN.name())
        .setParentActionId(actionId).setParentWorkflowId(id).build();
    workflowInstanceService.insertWorkflowInstance(child);
    instance = new WorkflowInstance.Builder().setType(SLOW_WORKFLOW_TYPE).setSignal(Optional.of(1)).setNextActivation(null)
        .build();
    workflowInstanceService.insertWorkflowInstance(instance);
    // insert demo bulk workflow with couple of children
    instance = workflowInstanceFactory.newWorkflowInstanceBuilder()
        .setType(DEMO_BULK_WORKFLOW_TYPE)
        .setState(SPLIT_WORK.name())
        .putStateVariable(VAR_CONCURRENCY, 2)
        .putStateVariable(VAR_CHILD_DATA, asList(1, 2, 3, 4, 5))
        .build();
    workflowInstanceService.insertWorkflowInstance(instance);
    instance = new WorkflowInstance.Builder().setType(FOREVER_WAITING_WORKFLOW_TYPE).build();
    workflowInstanceService.insertWorkflowInstance(instance);
  }
}
