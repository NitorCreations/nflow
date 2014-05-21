package com.nitorcreations.nflow.tests;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.FixMethodOrder;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.tests.demo.CreditApplicationWorkflow;

@FixMethodOrder(NAME_ASCENDING)
public class CreditApplicationWorkflowTest extends AbstractNflowTest {

  @Test
  public void t01_createCreditApplicationWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = "creditApplicationProcess";
    req.businessKey = UUID.randomUUID().toString();
    req.requestData = (new ObjectMapper()).valueToTree(
            new CreditApplicationWorkflow.CreditApplication("CUST123", new BigDecimal(100l)));
    req.externalId = UUID.randomUUID().toString();
    CreateWorkflowInstanceResponse resp = workflowInstanceResource.put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
  }

}
