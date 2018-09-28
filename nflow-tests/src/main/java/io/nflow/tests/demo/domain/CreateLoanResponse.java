package io.nflow.tests.demo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateLoanResponse extends AbstractResponse {

  public String accountNo;
  public boolean active;
  public int amount;
  public String applicationId;
  public String archiveId;
  public String clientId;
  // created
  public String loanId;
  public String productId;

}
