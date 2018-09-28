package io.nflow.tests.demo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryCreditApplicationResponse extends AbstractResponse {

  public String accountNo;
  public boolean active;
  public int amount;
  public String applicationId;
  public String clientId;
  // created
  public String productId;
  public String status;

}
