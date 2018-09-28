package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static io.nflow.tests.demo.workflow.CreditDecisionWorkflow.State.approved;
import static io.nflow.tests.demo.workflow.CreditDecisionWorkflow.State.decisionEngine;
import static io.nflow.tests.demo.workflow.CreditDecisionWorkflow.State.internalBlacklist;
import static io.nflow.tests.demo.workflow.CreditDecisionWorkflow.State.manualDecision;
import static io.nflow.tests.demo.workflow.CreditDecisionWorkflow.State.rejected;
import static io.nflow.tests.demo.workflow.CreditDecisionWorkflow.State.satQuery;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import io.nflow.tests.demo.domain.CreditDecisionData;

@Component
public class CreditDecisionWorkflow extends WorkflowDefinition<CreditDecisionWorkflow.State> {

  public static final String TYPE = "creditDecision";
  public static final String VAR_REQUEST_DATA = "requestData";

  public enum State implements io.nflow.engine.workflow.definition.WorkflowState {
    internalBlacklist(start, "Reject internally blacklisted customers"),
    decisionEngine(normal, "Check if application ok for decision engine"),
    satQuery(normal, "Query customer credit rating from SAT"),
    manualDecision(manual, "Manually approve or reject the application"),
    approved(end, "Credit Decision Approved"),
    rejected(end, "Credit Decision Rejected");

    private WorkflowStateType type;
    private String description;

    private State(WorkflowStateType type, String description) {
      this.type = type;
      this.description = description;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  public CreditDecisionWorkflow() {
    super(TYPE, internalBlacklist, manualDecision);
    setDescription("Approve or reject credit application");
    permit(internalBlacklist, decisionEngine);
    permit(decisionEngine, satQuery);
    permit(satQuery, approved);
    permit(satQuery, rejected);
  }

  public NextAction internalBlacklist(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(decisionEngine, "Customer not blacklisted");
  }

  public NextAction decisionEngine(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(satQuery, "Decision engine approves");
  }

  public NextAction satQuery(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(manualDecision, "SAT query failed");
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