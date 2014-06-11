package com.nitorcreations.nflow.rest.v0.converter;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceResponse;

@RunWith(MockitoJUnitRunner.class)
public class CreateWorkflowConverterTest {

  @Mock
  private ObjectMapper objectMapper;

  private CreateWorkflowConverter converter;

  @Before
  public void setup() {
    converter = new CreateWorkflowConverter(objectMapper);
  }

  @Test
  public void convertAndValidateWorks() throws JsonProcessingException {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.activationTime = DateTime.now();
    req.businessKey = "businessKey";
    req.externalId = "externalId";
    req.requestData = mock(JsonNode.class);
    req.type = "wfType";
    WorkflowInstance i = converter.convertAndValidate(req);
    assertThat(i.nextActivation, equalTo(req.activationTime));
    assertThat(i.businessKey, equalTo(req.businessKey));
    assertThat(i.externalId, equalTo(req.externalId));
    assertThat(i.type, equalTo(req.type));
  }

  @Test
  public void convertAndValidateWorksWithMinimalData() throws JsonProcessingException {
    CreateWorkflowInstanceRequest req = new CreateWorkflowInstanceRequest();
    req.type = "wfType";
    WorkflowInstance i = converter.convertAndValidate(req);
    assertThat(i.nextActivation, notNullValue(DateTime.class));
    assertThat(i.businessKey, nullValue(String.class));
    assertThat(i.externalId, nullValue(String.class));
    assertThat(i.type, equalTo(req.type));
  }

  @Test
  public void convertWorks() {
    WorkflowInstance i = new WorkflowInstance.Builder().setId(1).setType("dummy").setBusinessKey("businessKey")
        .setExternalId("externalId").build();
    CreateWorkflowInstanceResponse resp = converter.convert(i);
    assertThat(resp.id, equalTo(i.id));
    assertThat(resp.type, equalTo(i.type));
    assertThat(resp.businessKey, equalTo(i.businessKey));
    assertThat(resp.externalId, equalTo(i.externalId));
  }

}
