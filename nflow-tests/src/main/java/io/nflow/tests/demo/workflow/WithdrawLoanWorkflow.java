package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.curated.State;
import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.tests.demo.domain.CreateLoanResponse;
import io.nflow.tests.demo.domain.QueryCreditApplicationResponse;

@Component
public class WithdrawLoanWorkflow extends WorkflowDefinition {

  private static final String CREDIT_APPLICATION_KEY = "credit_application_key";
  private static final String LOAN_KEY = "loan_key";
  private static final String COMPLETED_KEY = "completed_key";

  private static final WorkflowState CREATE_LOAN = new State("createLoan", start, "Start process by creating the loan");
  private static final WorkflowState TRANSFER_MONEY = new State("transferMoney", "Transfer money to deposit account");
  private static final WorkflowState TRANSFER_MONEY_FAILED = new State("transferMoneyFailed",
      "Transfering money failed, reverse creating loan");
  private static final WorkflowState UPDATE_CREDIT_APPLICATION = new State("updateCreditApplication",
      "Update the credit application state");
  private static final WorkflowState MANUAL_PROCESSING = new State("manualProcessing", manual,
      "Process must be handled manually because of an unexpected situation");
  private static final WorkflowState DONE = new State("done", end, "Credit application has been completed.");

  @Inject
  public WithdrawLoanWorkflow() {
    super("withdrawLoan", CREATE_LOAN, MANUAL_PROCESSING);
    setName("Withdraw loan");
    setDescription("Creates loan, deposits the money and updates credit application");
    permit(CREATE_LOAN, TRANSFER_MONEY);
    permit(TRANSFER_MONEY, UPDATE_CREDIT_APPLICATION, TRANSFER_MONEY_FAILED);
    permit(TRANSFER_MONEY_FAILED, MANUAL_PROCESSING);
    permit(UPDATE_CREDIT_APPLICATION, DONE);
  }

  public NextAction createLoan(@SuppressWarnings("unused") StateExecution execution,
      @StateVar(value = LOAN_KEY) Mutable<CreateLoanResponse> loan,
      @StateVar(value = CREDIT_APPLICATION_KEY) Mutable<QueryCreditApplicationResponse> application) {
    application.setVal(new QueryCreditApplicationResponse());
    loan.setVal(new CreateLoanResponse());
    return moveToState(TRANSFER_MONEY, "Loan created");
  }

  public NextAction transferMoney(StateExecution execution) {
    setApplicationCompleted(execution, true);
    return moveToState(UPDATE_CREDIT_APPLICATION, "Money transferred");
  }

  public NextAction updateCreditApplication(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(DONE, "Credit application updated");
  }

  public NextAction transferMoneyFailed(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(MANUAL_PROCESSING, "Loan cancelled");
  }

  private void setApplicationCompleted(StateExecution execution, boolean success) {
    execution.setVariable(COMPLETED_KEY, Boolean.toString(success));
  }
}
