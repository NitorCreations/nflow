package io.nflow.tests.demo.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import org.springframework.stereotype.Component;

import static io.nflow.tests.demo.workflow.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static java.util.Collections.singletonMap;

/**
 * Bulk child workflow executor that does not overflow the system.
 */
@Component
public class DemoBulkWorkflow extends BulkWorkflow {
  public static final String DEMO_BULK_WORKFLOW_TYPE = "demoBulk";

  public DemoBulkWorkflow() {
    super(DEMO_BULK_WORKFLOW_TYPE);
  }

  protected boolean splitWorkImpl(StateExecution execution, JsonNode data) {
    data.forEach(childData -> {
      WorkflowInstance child = new WorkflowInstance.Builder()
              .setType(DEMO_WORKFLOW_TYPE)
              .setNextActivation(null)
              .setStateVariables(singletonMap("requestData", childData.asText()))
              .build();
      execution.addChildWorkflows(child);
    });
    return true;
  }
}
