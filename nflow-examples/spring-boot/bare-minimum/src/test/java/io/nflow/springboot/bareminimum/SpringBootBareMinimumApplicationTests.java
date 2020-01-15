package io.nflow.springboot.bareminimum;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringBootBareMinimumApplicationTests {

  @Inject
  private WorkflowInstanceService workflowInstances;

  @Inject
  private WorkflowInstanceFactory workflowInstanceFactory;

  @Test
  public void contextLoadsAndInstanceCanBeAdded() {
    workflowInstances.insertWorkflowInstance(workflowInstanceFactory.newWorkflowInstanceBuilder()
        .setType(ExampleWorkflow.TYPE)
        .setExternalId("example")
        .putStateVariable(ExampleWorkflow.VAR_COUNTER, 0)
        .build());
  }

}
