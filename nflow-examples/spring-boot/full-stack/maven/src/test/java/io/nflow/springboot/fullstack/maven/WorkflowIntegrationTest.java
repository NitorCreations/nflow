package io.nflow.springboot.fullstack.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

@SpringBootTest
class WorkflowIntegrationTest {

  @Inject
  WorkflowInstanceService workflowInstanceService;

  @Inject
  WorkflowInstanceFactory workflowInstanceFactory;

  @Test
  void workflowExecutesSuccessfully() throws InterruptedException {
    long id = workflowInstanceService.insertWorkflowInstance(
        workflowInstanceFactory.newWorkflowInstanceBuilder()
            .setType(ExampleWorkflow.TYPE)
            .setExternalId("integration-test-" + System.currentTimeMillis())
            .putStateVariable(ExampleWorkflow.VAR_COUNTER, 0)
            .build());

    WorkflowInstance instance = null;
    long deadline = System.currentTimeMillis() + 10_000;
    while (System.currentTimeMillis() < deadline) {
      instance = workflowInstanceService.getWorkflowInstance(id, Collections.emptySet(), null);
      if (instance.status == WorkflowInstance.WorkflowInstanceStatus.inProgress) break;
      Thread.sleep(200);
    }

    assertNotNull(instance);
    assertEquals(WorkflowInstance.WorkflowInstanceStatus.inProgress, instance.status);
  }
}
