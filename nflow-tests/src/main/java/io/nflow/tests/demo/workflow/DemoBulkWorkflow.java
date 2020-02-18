package io.nflow.tests.demo.workflow;

import static io.nflow.tests.demo.workflow.DemoWorkflow.DEMO_WORKFLOW_TYPE;
import static java.util.stream.StreamSupport.stream;
import static org.joda.time.DateTime.now;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import io.nflow.engine.workflow.curated.BulkWorkflow;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.instance.WorkflowInstance;

/**
 * Bulk child workflow executor that does not overflow the system.
 */
@Component
public class DemoBulkWorkflow extends BulkWorkflow {

  public static final String DEMO_BULK_WORKFLOW_TYPE = "demoBulk";

  public DemoBulkWorkflow() {
    super(DEMO_BULK_WORKFLOW_TYPE);
  }

  @Override
  protected boolean splitWorkImpl(StateExecution execution, JsonNode data) {
    if (data.size() == 0) {
      return false;
    }
    execution.addChildWorkflows(stream(data.spliterator(), false).map(this::createInstance).toArray(WorkflowInstance[]::new));
    return true;
  }

  private WorkflowInstance createInstance(JsonNode childData) {
    return new WorkflowInstance.Builder() //
        .setType(DEMO_WORKFLOW_TYPE) //
        .setNextActivation(null) //
        .putStateVariable("requestData", childData.asText()) //
        .build();
  }

  @Override
  protected DateTime waitForChildrenUntil() {
    return now().plusSeconds(10);
  }

  @Override
  protected DateTime waitForChildrenToCompleteUntil() {
    return now().plusSeconds(10);
  }

}
