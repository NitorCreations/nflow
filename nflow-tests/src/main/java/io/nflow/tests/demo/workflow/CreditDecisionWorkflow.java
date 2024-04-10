package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.curated.State;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.tests.demo.domain.CreditDecisionData;

@Component
public class CreditDecisionWorkflow extends WorkflowDefinition {

  public static final String CREDIT_DECISION_TYPE = "creditDecision";
  public static final String VAR_REQUEST_DATA = "requestData";

  private static final WorkflowState INTERNAL_BLACKLIST = new State("internalBlacklist", start,
      "Reject internally blacklisted customers");
  private static final WorkflowState DECISION_ENGINE = new State("decisionEngine",
      "Check if application ok for decision engine");
  private static final WorkflowState SAT_QUERY = new State("satQuery", "Query customer credit rating from SAT");
  private static final WorkflowState MANUAL_DECISION = new State("manualDecision", manual,
      "Manually approve or reject the application");
  static final WorkflowState APPROVED = new State("approved", end, "Credit Decision Approved");
  static final WorkflowState REJECTED = new State("rejected", end, "Credit Decision Rejected");

  @SuppressWarnings("this-escape")
  public CreditDecisionWorkflow() {
    super(CREDIT_DECISION_TYPE, INTERNAL_BLACKLIST, MANUAL_DECISION);
    setDescription("Approve or reject credit application");
    permit(INTERNAL_BLACKLIST, DECISION_ENGINE);
    permit(DECISION_ENGINE, SAT_QUERY);
    permit(SAT_QUERY, APPROVED);
    permit(SAT_QUERY, REJECTED);
  }

  public NextAction internalBlacklist(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(DECISION_ENGINE, "Customer not blacklisted");
  }

  public NextAction decisionEngine(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(SAT_QUERY, "Decision engine approves");
  }

  public NextAction satQuery(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(MANUAL_DECISION, "SAT query failed");
  }

  public void approved(StateExecution execution,
      @SuppressWarnings("unused") @StateVar(value = VAR_REQUEST_DATA) CreditDecisionData requestData) {
    execution.wakeUpParentWorkflow();
  }

  public void rejected(StateExecution execution,
      @SuppressWarnings("unused") @StateVar(value = VAR_REQUEST_DATA) CreditDecisionData requestData) {
    execution.wakeUpParentWorkflow();
  }

}