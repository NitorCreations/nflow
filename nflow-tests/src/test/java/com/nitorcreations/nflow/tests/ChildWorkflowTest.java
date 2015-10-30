package com.nitorcreations.nflow.tests;

import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;

import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.tests.demo.FibonacciWorkflow;
import com.nitorcreations.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class ChildWorkflowTest extends AbstractNflowTest {
    @ClassRule
    public static NflowServerRule server = new NflowServerRule.Builder().build();

    private static int workflowId;

    public ChildWorkflowTest() {
        super(server);
    }

    @Test
    public void t01_startFibonacciWorkflow() {
        CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
        req.type = "fibonacci";
        req.requestData = nflowObjectMapper().valueToTree(new FibonacciWorkflow.FiboData(5));
        CreateWorkflowInstanceResponse resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
        assertThat(resp.id, notNullValue());
        workflowId = resp.id;
    }

    @Test(timeout = 30000)
    public void t02_checkFibonacciWorkflowComputesCorrectResult() throws InterruptedException {
        ListWorkflowInstanceResponse response = getWorkflowInstance(workflowId, FibonacciWorkflow.State.done.name());
        assertTrue(response.stateVariables.containsKey("result"));
        assertEquals(8, response.stateVariables.get("result"));
    }
}
