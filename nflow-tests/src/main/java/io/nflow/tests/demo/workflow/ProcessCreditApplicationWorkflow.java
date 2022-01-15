package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.NextAction.moveToStateAfter;
import static io.nflow.engine.workflow.definition.NextAction.retryAfter;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static io.nflow.tests.demo.workflow.CreditDecisionWorkflow.APPROVED;
import static io.nflow.tests.demo.workflow.CreditDecisionWorkflow.CREDIT_DECISION_TYPE;
import static io.nflow.tests.demo.workflow.CreditDecisionWorkflow.REJECTED;
import static io.nflow.tests.demo.workflow.CreditDecisionWorkflow.VAR_REQUEST_DATA;
import static org.joda.time.DateTime.now;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.curated.State;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.tests.demo.domain.CreateCreditApplicationRequest;
import io.nflow.tests.demo.domain.CreateLoanResponse;
import io.nflow.tests.demo.domain.CreditDecisionData;
import io.nflow.tests.demo.domain.QueryCreditApplicationResponse;

@Component
public class ProcessCreditApplicationWorkflow extends AbstractWorkflowDefinition {

  private static final String CREDIT_APPLICATION_KEY = "credit_application_key";
  private static final String LOAN_KEY = "loan_key";
  private static final String CREDIT_DECISION_RESULT = "credit_decision_result";

  private static final WorkflowState CREATE_CREDIT_APPLICATION = new State("createCreditApplication", start,
      "Create new credit application");
  private static final WorkflowState START_CREDIT_DECISION_WORKFLOW = new State("startCreditDecisionWorkflow",
      "Start credit decision workflow");
  private static final WorkflowState WAIT_CREDIT_DECISION_WORKFLOW = new State("waitCreditDecisionWorkflow",
      "Poll for result of credit decision process");
  private static final WorkflowState CREATE_LOAN = new State("createLoan", "Create the loan based on application");
  private static final WorkflowState TRANSFER_MONEY = new State("transferMoney", "Transfer money to deposit account");
  private static final WorkflowState TRANSFER_MONEY_FAILED = new State("transferMoneyFailed",
      "Transfering money failed, reverse creating loan");
  private static final WorkflowState UPDATE_CREDIT_APPLICATION = new State("updateCreditApplication",
      "Update the credit application state");
  private static final WorkflowState MANUAL_PROCESSING = new State("manualProcessing", manual,
      "Process must be handled manually because of an unexpected situation");
  private static final WorkflowState DONE = new State("done", end, "Credit application has been completed.");

  public ProcessCreditApplicationWorkflow() {
    super("processCreditApplication", CREATE_CREDIT_APPLICATION, MANUAL_PROCESSING);
    setName("Process credit application");
    setDescription("Makes credit decision, creates loan, deposits the money and updates credit application");
    permit(CREATE_CREDIT_APPLICATION, START_CREDIT_DECISION_WORKFLOW);
    permit(START_CREDIT_DECISION_WORKFLOW, WAIT_CREDIT_DECISION_WORKFLOW);
    permit(WAIT_CREDIT_DECISION_WORKFLOW, CREATE_LOAN);
    permit(WAIT_CREDIT_DECISION_WORKFLOW, UPDATE_CREDIT_APPLICATION);
    permit(CREATE_LOAN, TRANSFER_MONEY);
    permit(TRANSFER_MONEY, UPDATE_CREDIT_APPLICATION, TRANSFER_MONEY_FAILED);
    permit(TRANSFER_MONEY_FAILED, MANUAL_PROCESSING);
    permit(UPDATE_CREDIT_APPLICATION, DONE);
  }

  public NextAction createCreditApplication(StateExecution execution,
      @StateVar(readOnly = true, value = "requestData") CreateCreditApplicationRequest request,
      @StateVar(value = CREDIT_APPLICATION_KEY) Mutable<QueryCreditApplicationResponse> application) {
    request.processWorkflowId = execution.getWorkflowInstanceId();
    application.setVal(new QueryCreditApplicationResponse());
    return moveToState(START_CREDIT_DECISION_WORKFLOW, "Credit application created");
  }

  public NextAction startCreditDecisionWorkflow(StateExecution execution,
      @StateVar(value = CREDIT_APPLICATION_KEY) QueryCreditApplicationResponse application) {
    CreditDecisionData creditDecisionData = new CreditDecisionData();
    creditDecisionData.clientId = application.clientId;
    creditDecisionData.amount = application.amount;
    execution.addChildWorkflows(execution.workflowInstanceBuilder().setType(CREDIT_DECISION_TYPE)
        .setBusinessKey(application.applicationId).putStateVariable(VAR_REQUEST_DATA, creditDecisionData).build());
    return moveToStateAfter(WAIT_CREDIT_DECISION_WORKFLOW, now().plusMonths(1), "Credit decision request submitted");
  }

  public NextAction waitCreditDecisionWorkflow(StateExecution execution,
      @StateVar(value = CREDIT_DECISION_RESULT) Mutable<String> creditDecisionResult) {
    WorkflowInstance decisionWorkflow = execution.getAllChildWorkflows().get(0);
    creditDecisionResult.setVal(decisionWorkflow.state);
    if (APPROVED.name().equals(decisionWorkflow.state)) {
      return moveToState(CREATE_LOAN, "Credit decision approved");
    }
    if (REJECTED.name().equals(decisionWorkflow.state)) {
      return moveToState(UPDATE_CREDIT_APPLICATION, "Credit decision rejected");
    }
    return retryAfter(now().plusSeconds(20), "Credit decision workflow in state " + decisionWorkflow.state);
  }

  public NextAction createLoan(@SuppressWarnings("unused") StateExecution execution,
      @StateVar(value = LOAN_KEY) Mutable<CreateLoanResponse> loan) {
    CreateLoanResponse response = new CreateLoanResponse();
    response.success = true;
    loan.setVal(response);
    return moveToState(TRANSFER_MONEY, "Loan created");
  }

  public NextAction transferMoney(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(UPDATE_CREDIT_APPLICATION, "Money transferred");
  }

  public NextAction updateCreditApplication(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(DONE, "Credit application updated");
  }

  public NextAction transferMoneyFailed(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(MANUAL_PROCESSING, "Loan cancelled");
  }
}
