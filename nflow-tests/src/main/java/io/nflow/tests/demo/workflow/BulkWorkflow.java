package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.finished;
import static io.nflow.tests.demo.workflow.BulkWorkflow.State.done;
import static io.nflow.tests.demo.workflow.BulkWorkflow.State.error;
import static io.nflow.tests.demo.workflow.BulkWorkflow.State.splitWork;
import static io.nflow.tests.demo.workflow.BulkWorkflow.State.waitForChildrenToFinish;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.EnumSet.complementOf;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

/**
 * Bulk child workflow executor that does not overflow the system.
 */
@Component
public class BulkWorkflow extends WorkflowDefinition<BulkWorkflow.State> {

  public static final String BULK_WORKFLOW_TYPE = "bulk";
  public static final String VAR_CHILD_DATA = "childData";
  public static final String VAR_CONCURRENCY = "concurrency";

  private static final EnumSet<WorkflowInstanceStatus> RUNNING_STATES = complementOf(EnumSet.of(finished, created));
  private static final Logger logger = getLogger(BulkWorkflow.class);

  @Inject
  WorkflowInstanceService instanceService;

  public enum State implements WorkflowState {
    splitWork(start), waitForChildrenToFinish(normal), done(end), error(manual);

    private WorkflowStateType type;

    State(WorkflowStateType type) {
      this.type = type;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return name();
    }
  }

  protected BulkWorkflow(String type) {
    super(type, splitWork, error);
    setDescription("Executes child workflows in bulk but gracefully without effecting non-bulk tasks.");
    permit(splitWork, waitForChildrenToFinish);
    permit(waitForChildrenToFinish, done);
  }

  public BulkWorkflow() {
    this(BULK_WORKFLOW_TYPE);
  }

  public NextAction splitWork(StateExecution execution, @StateVar(value = VAR_CHILD_DATA, readOnly = true) JsonNode data) {
    boolean childrenFound = splitWorkImpl(execution, data);
    if (childrenFound) {
      return moveToState(waitForChildrenToFinish, "Running");
    }
    return retryAfter(waitForChildrenUntil(), "Waiting for child workflows");
  }

  protected boolean splitWorkImpl(StateExecution execution, @SuppressWarnings("unused") JsonNode data) {
    if (execution.getAllChildWorkflows().isEmpty()) {
      throw new RuntimeException(
          "No child workflows found - either add them before starting the parent or implement splitWorkflowImpl");
    }
    return true;
  }

  protected DateTime waitForChildrenUntil() {
    return now().plusMinutes(1);
  }

  public NextAction waitForChildrenToFinish(StateExecution execution,
      @StateVar(value = VAR_CONCURRENCY, readOnly = true) int concurrency) {
    List<WorkflowInstance> childWorkflows = execution.getAllChildWorkflows();
    long completed = 0;
    long running = 0;
    for (WorkflowInstance child : childWorkflows) {
      if (child.status == finished) {
        completed++;
      } else if (isRunning(child)) {
        running++;
      }
    }
    if (completed == childWorkflows.size()) {
      return moveToState(done, "All children completed");
    }
    long toStart = min(max(1, concurrency) - running, childWorkflows.size() - completed);
    if (toStart > 0) {
      childWorkflows.stream().filter(this::isInInitialState).limit(toStart).forEach(this::wakeup);
      logger.info("Started " + toStart + " child workflows");
    }
    long progress = completed * 100 / childWorkflows.size();
    return retryAfter(waitForChildrenToCompleteUntil(), "Waiting for child workflows to complete - " + progress + "% done");
  }

  private void wakeup(WorkflowInstance instance) {
    instanceService.wakeupWorkflowInstance(instance.id, emptyList());
  }

  protected boolean isRunning(WorkflowInstance instance) {
    return RUNNING_STATES.contains(instance.status);
  }

  private boolean isInInitialState(WorkflowInstance instance) {
    return instance.status == created;
  }

  protected DateTime waitForChildrenToCompleteUntil() {
    return now().plusMinutes(15);
  }

}
