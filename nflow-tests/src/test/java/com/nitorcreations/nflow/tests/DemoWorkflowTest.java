package com.nitorcreations.nflow.tests;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import org.junit.FixMethodOrder;
import org.junit.Test;

import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowInstanceResponse;

@FixMethodOrder(NAME_ASCENDING)
public class DemoWorkflowTest extends AbstractNflowTest {

  @Test
  public void t01_startDemoWorkflow() {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = "demo";
    req.businessKey = "1";
    CreateWorkflowInstanceResponse resp = workflowInstanceResource.put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
  }

  @Test
  public void t02_queryDemoWorkflowHistory() throws Exception {
    sleep(5000);
    ListWorkflowInstanceResponse[] instances = workflowInstanceResource.query("type", "demo")
            .query("include", "actions").get(ListWorkflowInstanceResponse[].class);
    assertThat(instances.length, is(1));
    assertThat(instances[0].state, is("done"));
    assertThat(instances[0].nextActivation, nullValue());
    assertThat(instances[0].actions.size(), is(3));
  }

}
