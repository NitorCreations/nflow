package com.nitorcreations.nflow.tests.demo;

import static com.nitorcreations.nflow.tests.demo.SpringApplicationContext.applicationContext;

import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.jetty.StartNflow;
import com.nitorcreations.nflow.metrics.NflowMetricsContext;

public class DemoServer {
  public static void main(String[] args) throws Exception {
    new StartNflow().registerSpringContext(NflowMetricsContext.class, SpringApplicationContext.class, DemoWorkflow.class)
        .startJetty(7500, "local", "jmx");
    WorkflowInstanceService workflowInstanceService = applicationContext.getBean(WorkflowInstanceService.class);
    WorkflowInstance instance = new WorkflowInstance.Builder().setType("demo").setState("begin").build();
    workflowInstanceService.insertWorkflowInstance(instance);
  }
}
