package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static io.nflow.tests.demo.workflow.WithdrawLoanWorkflow.State.createLoan;
import static io.nflow.tests.demo.workflow.WithdrawLoanWorkflow.State.done;
import static io.nflow.tests.demo.workflow.WithdrawLoanWorkflow.State.manualProcessing;
import static io.nflow.tests.demo.workflow.WithdrawLoanWorkflow.State.transferMoney;
import static io.nflow.tests.demo.workflow.WithdrawLoanWorkflow.State.transferMoneyFailed;
import static io.nflow.tests.demo.workflow.WithdrawLoanWorkflow.State.updateCreditApplication;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.definition.Mutable;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowStateType;
import io.nflow.tests.demo.domain.CreateLoanResponse;
import io.nflow.tests.demo.domain.QueryCreditApplicationResponse;

@Component
public class WithdrawLoanWorkflow extends WorkflowDefinition<WithdrawLoanWorkflow.State> {

  private static final String CREDIT_APPLICATION_KEY = "credit_application_key";
  private static final String LOAN_KEY = "loan_key";
  private static final String COMPLETED_KEY = "completed_key";

  public enum State implements io.nflow.engine.workflow.definition.WorkflowState {
    createLoan(start, "Start process by creating the loan"),
    transferMoney(normal, "Transfer money to deposit account"),
    transferMoneyFailed(normal, "Transfering money failed, reverse creating loan"),
    updateCreditApplication(normal, "Update the credit application state"),
    manualProcessing(manual, "Process must be handled manually because of an unexpected situation"),
    done(end, "Credit application has been completed.");

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

  @Inject
  public WithdrawLoanWorkflow() {
    super("withdrawLoan", createLoan, manualProcessing);
    setName("Withdraw loan");
    setDescription("Creates loan, deposits the money and updates credit application");
    permit(createLoan, transferMoney);
    permit(transferMoney, updateCreditApplication, transferMoneyFailed);
    permit(transferMoneyFailed, manualProcessing);
    permit(updateCreditApplication, done);
  }

  public NextAction createLoan(@SuppressWarnings("unused") StateExecution execution,
      @StateVar(value = LOAN_KEY) Mutable<CreateLoanResponse> loan,
      @StateVar(value = CREDIT_APPLICATION_KEY) Mutable<QueryCreditApplicationResponse> application) {
    application.setVal(new QueryCreditApplicationResponse());
    loan.setVal(new CreateLoanResponse());
    return moveToState(transferMoney, "Loan created");
  }

  public NextAction transferMoney(StateExecution execution) {
    setApplicationCompleted(execution, true);
    return moveToState(updateCreditApplication, "Money transferred");
  }

  public NextAction updateCreditApplication(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(done, "Credit application updated");
  }

  public NextAction transferMoneyFailed(@SuppressWarnings("unused") StateExecution execution) {
    return moveToState(manualProcessing, "Loan cancelled");
  }

  private void setApplicationCompleted(StateExecution execution, boolean success) {
    execution.setVariable(COMPLETED_KEY, Boolean.toString(success));
  }

}
