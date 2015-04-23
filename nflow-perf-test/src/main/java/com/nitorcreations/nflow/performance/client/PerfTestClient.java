package com.nitorcreations.nflow.performance.client;

import static java.util.UUID.randomUUID;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.cxf.jaxrs.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;

/**
 * Client for sending messages to nFlow REST API in the performance tests.
 */
@Named
public class PerfTestClient {

  @Inject
  @Named("workflowInstance")
  private WebClient workflowInstanceResource;

  @Inject
  @Named("statistics")
  private WebClient statisticsResource;

  @Inject
  private ObjectMapper objectMapper;

  public CreateWorkflowInstanceResponse createWorkflow(WorkflowDefinition<?> def) {
    return createWorkflow(def.getType());
  }

  public CreateWorkflowInstanceResponse createWorkflow(String workflowType) {
    CreateWorkflowInstanceRequest request = new CreateWorkflowInstanceRequest();
    request.type = workflowType;
    request.businessKey = randomUUID().toString();
    request.externalId = randomUUID().toString();
    try {
      request.requestData = objectMapper.readTree("{\"customerId\":\"CUST123\",\"amount\":100.0,\"simulation\":false}");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return makeInstanceRequest(request, CreateWorkflowInstanceResponse.class);
  }

  public ListWorkflowInstanceResponse getWorkflowInstance(int instanceId, boolean fetchActions) {
    WebClient restReq = fromClient(workflowInstanceResource, true).path(Integer.toString(instanceId));
    if (fetchActions) {
      return restReq.query("include", "actions").get(ListWorkflowInstanceResponse.class);
    }
    return restReq.get(ListWorkflowInstanceResponse.class);
  }

  public StatisticsResponse getStatistics() {
    WebClient restReq = fromClient(statisticsResource, true);
    return restReq.get(StatisticsResponse.class);
  }

  private <T> T makeInstanceRequest(Object request, Class<T> responseClass) {
    return fromClient(workflowInstanceResource, true).put(request, responseClass);
  }
}
