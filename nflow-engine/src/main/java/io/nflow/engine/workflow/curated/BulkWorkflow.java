package io.nflow.engine.workflow.curated;

import static io.nflow.engine.workflow.curated.BulkWorkflow.State.done;
import static io.nflow.engine.workflow.curated.BulkWorkflow.State.error;
import static io.nflow.engine.workflow.curated.BulkWorkflow.State.splitWork;
import static io.nflow.engine.workflow.curated.BulkWorkflow.State.waitForChildrenToFinish;
import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static io.nflow.engine.workflow.definition.WorkflowStateType.wait;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.finished;
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
import io.nflow.engine.workflow.curated.BulkWorkflow.State;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings.Builder;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

/**
 * Bulk child workflow executor that does not overflow the system.
 */
@Component
public class BulkWorkflow extends WorkflowDefinition<State> {

  /**
   * The type of default bulk workflow.
   */
  public static final String BULK_WORKFLOW_TYPE = "bulk";

  /**
   * State variable name for child data.
   */
  public static final String VAR_CHILD_DATA = "childData";

  /**
   * State variable to define the maximum concurrency for executing child workflows.
   */
  public static final String VAR_CONCURRENCY = "concurrency";

  private static final EnumSet<WorkflowInstanceStatus> RUNNING_STATES = complementOf(EnumSet.of(finished, created));
  private static final Logger logger = getLogger(BulkWorkflow.class);

  @Inject
  private WorkflowInstanceService instanceService;

  /**
   * Bulk workflow states.
   */
  public enum State implements io.nflow.engine.workflow.definition.WorkflowState {
    splitWork(start), waitForChildrenToFinish(wait), done(end), error(manual);

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

  /**
   * Extend bulk workflow definition.
   *
   * @param type
   *          The type of the workflow.
   */
  protected BulkWorkflow(String type) {
    super(type, splitWork, error, new Builder().setMaxRetries(Integer.MAX_VALUE).build());
    setDescription("Executes child workflows in bulk but gracefully without effecting non-bulk tasks.");
    permit(splitWork, waitForChildrenToFinish);
    permit(waitForChildrenToFinish, done);
  }

  /**
   * Create bulk workflow definition.
   */
  public BulkWorkflow() {
    this(BULK_WORKFLOW_TYPE);
  }

  /**
   * Call {@link splitWorkImpl} to create new child workflows.
   *
   * @param execution
   *          State execution context.
   * @param data
   *          Child data.
   * @return Action to start waiting for children to finish or action to wait for children to be created.
   */
  public NextAction splitWork(StateExecution execution, @StateVar(value = VAR_CHILD_DATA, readOnly = true) JsonNode data) {
    boolean childrenFound = splitWorkImpl(execution, data);
    if (childrenFound) {
      return moveToState(waitForChildrenToFinish, "Running");
    }
    return retryAfter(waitForChildrenUntil(), "Waiting for child workflows");
  }

  /**
   * Override this to create child workflows or add the children before starting the parent.
   *
   * @param execution
   *          State execution context.
   * @param data
   *          Child data.
   * @return True to start processing the children, false to wait for children to be created.
   * @throws RuntimeException
   *           Thrown by default implementation if children are not created before the parent is started.
   */
  protected boolean splitWorkImpl(StateExecution execution, JsonNode data) {
    if (execution.getAllChildWorkflows().isEmpty()) {
      throw new RuntimeException("No child workflows found for workflow instance " + execution.getWorkflowInstanceId()
          + " - either add them before starting the parent or implement splitWorkflowImpl");
    }
    return true;
  }

  /**
   * Override this to customize the time to wait for new children before waking up. Default is one hour.
   *
   * @return Time when parent should wake up to check if new children have been added.
   */
  protected DateTime waitForChildrenUntil() {
    return now().plusHours(1);
  }

  /**
   * Check if all child workflows have finished. Start new children if needed and allowed by concurrency limit.
   *
   * @param execution
   *          State execution context.
   * @param concurrency
   *          The maximum number of child workflows to start.
   * @return Action to retry this state at the time returned by {@link waitForChildrenToCompleteUntil}.
   */
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
      logger.info("Started {} child workflows", toStart);
    }
    long progress = completed * 100 / childWorkflows.size();
    return retryAfter(waitForChildrenToCompleteUntil(), "Waiting for child workflows to complete - " + progress + "% done");
  }

  private void wakeup(WorkflowInstance instance) {
    instanceService.wakeupWorkflowInstance(instance.id, emptyList());
  }

  /**
   * Override this to determine if the child workflow is running or not. The default implementation returns true if the instance
   * state is finished or created.
   *
   * @param instance
   *          The child workflow instance to check.
   * @return True if the child is running, false otherwise.
   */
  protected boolean isRunning(WorkflowInstance instance) {
    return RUNNING_STATES.contains(instance.status);
  }

  private boolean isInInitialState(WorkflowInstance instance) {
    return instance.status == created;
  }

  /**
   * Override this to customize the time to wait for children to finish. Default is 15 minutes. This is a safety mechanism only,
   * as when the children normally go to finished state, they will automatically wake up the parent workflow.
   *
   * @return The time when parent should wake up to check if the children are finished.
   */
  protected DateTime waitForChildrenToCompleteUntil() {
    return now().plusMinutes(15);
  }

}
