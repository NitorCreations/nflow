package io.nflow.tests.demo.workflow.perf;

import com.fasterxml.jackson.databind.JsonNode;
import io.nflow.engine.workflow.curated.BulkWorkflow;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import org.springframework.stereotype.Component;

@Component
public class PerfBulkWorkflow extends BulkWorkflow {

  public static final String TYPE = "perfBulk";

  public PerfBulkWorkflow() {
    super(TYPE);
    setDescription("Perf bulk workflow");
  }

  protected boolean splitWorkImpl(StateExecution execution, JsonNode data) {
    int count = data.get("count").asInt();
    String type = data.get("type").asText();
    WorkflowInstance.Builder childWorkflow = new WorkflowInstance.Builder().setType(type);
    WorkflowInstance[] children = new WorkflowInstance[count];
    for (int i=0; i<count; ++i) {
      children[i] = childWorkflow.build();
    }
    execution.addChildWorkflows(children);
    return true;
  }
}
